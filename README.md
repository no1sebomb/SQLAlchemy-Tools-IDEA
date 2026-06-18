# SQLAlchemy Tools for IntelliJ IDEA

![Plugin Icon](src/main/resources/META-INF/pluginIcon_dark.svg)

A lightweight PyCharm plugin that adds productivity tools for working with SQLAlchemy 2.0.

It provides ready-to-use file templates, live templates, and code generation helpers for faster and more consistent ORM development.

> [!NOTE]
> Some parts of the source code were written with the help of AI, 
> since I have no experience with Kotlin and the IntelliJ Platform SDK. 
> However, I have reviewed and tested all generated code to ensure it works as intended.

---

## ✨ Features

### 🚀 Model Generator Action
- Generate SQLAlchemy models via IDE action
- Interactive input for model name
- Extensible foundation for future schema-based generation

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
