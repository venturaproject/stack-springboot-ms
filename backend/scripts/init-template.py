#!/usr/bin/env python3
"""Apply the initial app name and Java namespace to a fresh repository copy."""

from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path


EXCLUDED_DIRS = {
    ".git", ".gradle", ".pnpm-store", "__pycache__", "build", "coverage", "dist", "media", "node_modules"
}
EXCLUDED_FILES = {".env", ".env.local"}
PACKAGE_RE = re.compile(r"^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$")


def slugify(value: str) -> str:
    return re.sub(r"-+", "-", re.sub(r"[^a-z0-9]+", "-", value.lower())).strip("-")


def prompt_value(label: str, supplied: str | None, default: str) -> str:
    if supplied is not None:
        return supplied.strip()
    if not sys.stdin.isatty():
        raise ValueError(f"Pass --{label.replace(' ', '-')} when running non-interactively")
    value = input(f"{label} [{default}]: ").strip()
    return value or default


def walk_files(root: Path):
    for current, dirs, files in os.walk(root):
        dirs[:] = [name for name in dirs if name not in EXCLUDED_DIRS]
        for name in files:
            if name in EXCLUDED_FILES:
                continue
            yield Path(current) / name


def replace_text_files(root: Path, replacements: dict[str, str]) -> None:
    for path in walk_files(root):
        try:
            original = path.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        updated = original
        for old, new in replacements.items():
            updated = updated.replace(old, new)
        if updated != original:
            path.write_text(updated, encoding="utf-8")


def move_java_packages(root: Path, package: str) -> int:
    target_relative = Path(*package.split("."))
    moves: list[tuple[Path, Path]] = []
    for current, dirs, _ in os.walk(root):
        dirs[:] = [name for name in dirs if name not in EXCLUDED_DIRS]
        source = Path(current) / "com" / "boilerplate"
        if "java" not in source.parts or not source.is_dir():
            continue
        java_root = source.parents[1]
        target = java_root / target_relative
        if source == target:
            continue
        if target.is_relative_to(source):
            raise FileExistsError(f"Target package cannot be nested under its source: {target}")
        if target.exists():
            raise FileExistsError(f"Target package already exists: {target}")
        moves.append((source, target))

    moved: list[tuple[Path, Path]] = []
    try:
        for source, target in moves:
            target.parent.mkdir(parents=True, exist_ok=True)
            source.rename(target)
            moved.append((source, target))
    except OSError:
        for source, target in reversed(moved):
            source.parent.mkdir(parents=True, exist_ok=True)
            target.rename(source)
        raise
    return len(moved)


def replacements_for(name: str, package: str) -> dict[str, str]:
    slug = slugify(name)
    env_name = f'"{name}"' if any(character.isspace() for character in name) else name
    return {
        "com.boilerplate": package,
        "com/boilerplate": package.replace(".", "/"),
        "boilerplate-spring-microservices": slug,
        "boilerplate-spring": slug,
        "boilerplate_spring": slug.replace("-", "_"),
        "VITE_APP_NAME=Application": f"VITE_APP_NAME={env_name}",
        "VITE_APP_NAME:-Application": f"VITE_APP_NAME:-{name}",
        'VITE_APP_NAME="Application"': f'VITE_APP_NAME="{name}"',
        "|| 'Application'": f"|| '{name}'",
    }


def initialize_template(root: Path, name: str, package: str) -> tuple[int, str]:
    slug = slugify(name)
    if not name or not slug:
        raise ValueError("The app name must contain at least one letter or digit")
    if not PACKAGE_RE.fullmatch(package):
        raise ValueError("The Java package must contain at least two lowercase identifier segments")

    moved = move_java_packages(root, package)
    replace_text_files(root, replacements_for(name, package))
    return moved, slug


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--name", help="Display name for the new application")
    parser.add_argument("--package", help="Base Java package, e.g. com.example.inventory")
    parser.add_argument("--yes", action="store_true", help="Skip the final confirmation")
    args = parser.parse_args()

    root = Path.cwd()
    try:
        name = prompt_value("app name", args.name, "Application")
        package = prompt_value("package", args.package, "com.example.application")
    except ValueError as error:
        parser.error(str(error))

    slug = slugify(name)
    if not name or not slug:
        parser.error("The app name must contain at least one letter or digit")
    if not PACKAGE_RE.fullmatch(package):
        parser.error("The Java package must contain at least two lowercase identifier segments")

    if not args.yes:
        print(f"This will modify source/configuration files under {root}.")
        if not sys.stdin.isatty() or input(f"Initialize '{name}' ({package})? [y/N] ").strip().lower() != "y":
            print("Cancelled; no files changed.")
            return 1

    try:
        moved, slug = initialize_template(root, name, package)
    except OSError as error:
        parser.error(f"Could not finish initialization: {error}")
    except ValueError as error:
        parser.error(str(error))

    print(f"Initialized '{name}' ({slug}); updated Java package in {moved} source roots.")
    print("Review the diff and run the full verification suite before committing.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
