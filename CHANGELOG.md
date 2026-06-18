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

## [0.0.2] - 2026-06-16

### Added

- New plugin icon.
- Support for older IntelliJ versions (2024.3+).

## [0.0.3] - 2026-06-16

### Added

- Support `backref` as alternative to `back_populates` in relationship links.

## [0.0.4] - 2026-06-19

### Added

- Create SQLAlchemy model from SQL (DDL)
- New icon for 'SQLAlchemy Model' action

### Fixed

- Fixed an issue in dependency configuration that made the plugin unavailable for PyCharm Pro.
