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
