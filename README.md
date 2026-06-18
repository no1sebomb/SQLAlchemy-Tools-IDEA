# SQLAlchemy Tools for IntelliJ IDEA

![Plugin Icon](src/main/resources/META-INF/pluginIcon_dark.svg)

A lightweight **PyCharm** plugin that adds productivity tools for working with **SQLAlchemy**.

It provides ready-to-use file templates, live templates, and code generation helpers for faster and more consistent ORM development.

> [!NOTE]
> Some parts of the source code were written with the help of AI, 
> since I have no experience with Kotlin and the IntelliJ Platform SDK. 
> However, I have reviewed and tested all generated code to ensure it works as intended.

---

<!-- Plugin description -->
Design your tables visually and generate clean, modern **SQLAlchemy** models in seconds — no boilerplate typing required.

## ✨ Features

### 🚀 Interactive Model Generator
A DataGrip-style "Create Model" dialog lets you build a model the way you think about a table:
- Tree-based editor for the table and its columns (add, remove, reorder, duplicate)
- Per-column options: type, primary key, unique, nullable, default expression and description
- SQL (DDL) mode: paste a `CREATE TABLE` statement and have the columns filled in automatically
- Smart naming with CamelCase / snake_case hints and Python validation
- Live, syntax-highlighted preview of the generated code with one-click copy
- Generates SQLAlchemy 2.0 style with `Mapped[]` and `mapped_column()` (legacy `Column()` optional)

### 🔗 Gutter Icons
- Visual indicators for relationships in the code
- Quick navigation between related models

### 📄 File Templates
- SQLAlchemy 2.0 declarative model template
- Automatic CamelCase class generation from file name
- Preconfigured base structure

### ⚡ Live Templates
Speed up ORM development with shortcuts for:
- Columns (Integer, String, etc.)
- Relationships (one-to-many, many-to-one)
- Common SQLAlchemy patterns

Designed to reduce repetitive typing and help maintain consistent SQLAlchemy code across projects.
<!-- Plugin description end -->

---

## 🧠 Designed for SQLAlchemy 2.0

This plugin uses modern SQLAlchemy patterns:
- `Mapped[]` typing
- `mapped_column()`
- Declarative Base style
- Python type hints-first approach

---

## 📦 Installation

### From JetBrains Marketplace

Plugin is available on [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/32172-sqlalchemy-tools).

Alternatively, you can open `Settings` > `Plugins` > `Marketplace` and search for **SQLAlchemy Tools**

### Manual Installation

1. Download latest release from [GitHub Releases](https://github.com/no1sebomb/SQLAlchemy-Tools-IDEA/releases)
2. In **IntelliJ IDEA**, go to `Settings` > `Plugins` > `Install Plugin from Disk...`
3. Select the downloaded `.zip` file and install.
