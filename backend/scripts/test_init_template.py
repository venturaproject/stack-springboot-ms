import importlib.util
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("init-template.py")
SPEC = importlib.util.spec_from_file_location("init_template", SCRIPT)
init_template = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(init_template)


class TemplateInitializerTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)

    def tearDown(self):
        self.temp.cleanup()

    def add_java_source(self, relative_root: str, filename: str) -> Path:
        package = self.root / relative_root / "com" / "boilerplate"
        package.mkdir(parents=True)
        source = package / filename
        source.write_text("package com.boilerplate;\n", encoding="utf-8")
        return source

    def test_updates_main_and_optional_service_packages_but_preserves_local_data_and_caches(self):
        self.add_java_source("backend/src/main/java", "Application.java")
        self.add_java_source("backend/services/catalog-service/src/main/java", "Catalog.java")
        readme = self.root / "README.md"
        readme.write_text("boilerplate-spring VITE_APP_NAME=Application\n", encoding="utf-8")

        env = self.root / ".env"
        env.write_text("APP_NAME=Application\n", encoding="utf-8")
        local_env = self.root / "frontend/.env.local"
        local_env.parent.mkdir(parents=True)
        local_env.write_text("VITE_APP_NAME=Personal\n", encoding="utf-8")
        cached_file = self.root / "backend/build/marker.txt"
        cached_file.parent.mkdir(parents=True)
        cached_file.write_text("com.boilerplate\n", encoding="utf-8")
        media_file = self.root / "backend/media/avatar.bin"
        media_file.parent.mkdir(parents=True)
        media_file.write_bytes(b"\x00\xffcom.boilerplate")

        moved, slug = init_template.initialize_template(self.root, "Inventory Portal", "com.example.inventory")

        self.assertEqual((moved, slug), (2, "inventory-portal"))
        self.assertEqual(
            (self.root / "backend/src/main/java/com/example/inventory/Application.java").read_text(),
            "package com.example.inventory;\n",
        )
        self.assertEqual(
            (self.root / "backend/services/catalog-service/src/main/java/com/example/inventory/Catalog.java")
            .read_text(),
            "package com.example.inventory;\n",
        )
        self.assertIn('VITE_APP_NAME="Inventory Portal"', readme.read_text())
        self.assertEqual(env.read_text(), "APP_NAME=Application\n")
        self.assertEqual(local_env.read_text(), "VITE_APP_NAME=Personal\n")
        self.assertEqual(cached_file.read_text(), "com.boilerplate\n")
        self.assertEqual(media_file.read_bytes(), b"\x00\xffcom.boilerplate")

    def test_is_idempotent_after_a_successful_initialization(self):
        self.add_java_source("backend/src/main/java", "Application.java")

        first = init_template.initialize_template(self.root, "Inventory", "com.example.inventory")
        second = init_template.initialize_template(self.root, "Inventory", "com.example.inventory")

        self.assertEqual(first, (1, "inventory"))
        self.assertEqual(second, (0, "inventory"))

    def test_detects_package_collisions_before_moving_any_source_root(self):
        original = self.add_java_source("backend/src/main/java", "Application.java")
        other = self.add_java_source("backend/services/catalog-service/src/main/java", "Catalog.java")
        collision = self.root / "backend/services/catalog-service/src/main/java/com/example/inventory"
        collision.mkdir(parents=True)
        (collision / "Existing.java").write_text("// existing\n", encoding="utf-8")

        with self.assertRaises(FileExistsError):
            init_template.initialize_template(self.root, "Inventory", "com.example.inventory")

        self.assertTrue(original.exists())
        self.assertTrue(other.exists())

    def test_rejects_invalid_inputs_without_mutating_files(self):
        source = self.add_java_source("backend/src/main/java", "Application.java")

        with self.assertRaises(ValueError):
            init_template.initialize_template(self.root, "!!!", "com.example.inventory")
        with self.assertRaises(ValueError):
            init_template.initialize_template(self.root, "Inventory", "Com.Example.Inventory")

        self.assertTrue(source.exists())
        self.assertEqual(source.read_text(), "package com.boilerplate;\n")


if __name__ == "__main__":
    unittest.main()
