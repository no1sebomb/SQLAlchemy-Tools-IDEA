<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# SQLAlchemy Tools Changelog

## [Unreleased]

### Added

- **Interactive SQLAlchemy model generator** — a DataGrip-style "Create Model" dialog to design tables and columns visually:
  - Tree-based editor with add / remove / reorder / duplicate actions for columns
  - Per-column options: type, primary key, unique, nullable, default expression and description
  - Distinct icons for primary key and not-null columns
  - CamelCase / snake_case naming hints and Python validation (model name, attribute names, default expressions)
  - Live, syntax-highlighted preview of the generated code with copy-to-clipboard
  - SQLAlchemy 2.0 output with `Mapped[]` and `mapped_column()` (optional legacy `Column()`)
- File templates for SQLAlchemy model definition
- Live templates for columns and relationships

## [0.0.1] - 2026-06-08

### Added

- Gutter icons for relationship links in the model code, allowing quick navigation between related models.

### Fixed

- Fixed input validation for different column name - now it only accepts valid SQL column name.
- Fixed `colint` live template.
