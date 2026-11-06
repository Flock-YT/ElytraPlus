"""Release tests use a local gh stub; no tag or external release is created."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class ReleaseTest(unittest.TestCase):
    def run_release(self, mutate=None):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            version = '2.0.2'
            names = ['ElytraPlus-' + version + '.jar', 'ElytraPlus-Fabric-' + version + '.jar']
            hashes = []
            for name in names:
                content = name.encode()
                (directory / name).write_bytes(content)
                digest = hashlib.sha256(content).hexdigest()
                hashes.append(digest)
                (directory / (name + '.sha256')).write_text(digest + '  ' + name + '\n')
            report_name = 'ElytraPlus-Fabric-' + version + '-verification.json'
            report = {'scope': 'code', 'passed': True, 'version': version, 'jarSha256': hashes[1]}
            if mutate:
                mutate(directory, names, report)
            (directory / report_name).write_text(json.dumps(report))
            stub = directory / 'gh'
            stub.write_text('#!/bin/sh\nprintf "%s\\n" "$@" > "$RELEASE_ARGS"\n')
            stub.chmod(0o755)
            arguments = directory / 'arguments'
            env = dict(os.environ, PATH=str(directory) + os.pathsep + os.environ['PATH'],
                       GITHUB_SHA='a' * 40, RELEASE_ARGS=str(arguments))
            result = subprocess.run(['bash', str(ROOT / 'scripts/create-release.sh'), version, str(directory), *hashes],
                                    env=env, capture_output=True, text=True)
            return result, arguments.read_text().splitlines() if arguments.exists() else []

    def test_uploads_both_verified_artifacts_and_report_without_rebuild(self):
        result, args = self.run_release()
        self.assertEqual(0, result.returncode, result.stderr)
        for name in ('ElytraPlus-2.0.2.jar', 'ElytraPlus-Fabric-2.0.2.jar', 'ElytraPlus-Fabric-2.0.2-verification.json'):
            self.assertIn(name, args)
        self.assertIn('--target', args)

    def test_changed_artifacts_are_rejected_before_publication(self):
        for index in (0, 1):
            with self.subTest(platform=index):
                result, args = self.run_release(lambda directory, names, report: (directory / names[index]).write_bytes(b'changed'))
                self.assertNotEqual(0, result.returncode)
                self.assertEqual([], args)

    def test_wrong_report_hash_version_scope_and_failed_checks_are_rejected(self):
        for key, value in [('jarSha256', '0' * 64), ('version', '2.0.1'), ('scope', 'game'), ('passed', False)]:
            with self.subTest(key=key):
                result, args = self.run_release(lambda directory, names, report: report.update({key: value}))
                self.assertNotEqual(0, result.returncode)
                self.assertEqual([], args)
