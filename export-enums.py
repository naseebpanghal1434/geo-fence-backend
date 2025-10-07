#!/usr/bin/env python3
"""
Script to export Java enums and constants to TypeScript files for Angular.
Parses Java files and generates corresponding TypeScript enum/const definitions.
"""

import os
import re
from pathlib import Path
from typing import List, Dict, Tuple

class JavaToTypeScriptExporter:
    def __init__(self, backend_path: str, output_path: str):
        self.backend_path = backend_path
        self.output_path = output_path
        self.exported_files = []

    def parse_standalone_enum(self, file_path: str) -> Tuple[str, List[str]]:
        """Parse a standalone Java enum file."""
        with open(file_path, 'r') as f:
            content = f.read()

        # Extract enum name
        enum_match = re.search(r'public\s+enum\s+(\w+)', content)
        if not enum_match:
            return None, []

        enum_name = enum_match.group(1)

        # Extract enum values
        enum_body = re.search(r'enum\s+\w+\s*\{([^}]+)\}', content)
        if not enum_body:
            return enum_name, []

        values = []
        for line in enum_body.group(1).split('\n'):
            line = line.strip()
            if line and not line.startswith('//') and not line.startswith('*'):
                # Remove trailing commas and semicolons
                value = re.sub(r'[,;].*', '', line).strip()
                if value and value.isupper():
                    values.append(value)

        return enum_name, values

    def parse_nested_enum(self, file_path: str) -> List[Tuple[str, str, List[str]]]:
        """Parse nested enums from entity files. Returns list of (parent_class, enum_name, values)."""
        with open(file_path, 'r') as f:
            content = f.read()

        # Extract parent class name
        class_match = re.search(r'public\s+class\s+(\w+)', content)
        if not class_match:
            return []

        parent_class = class_match.group(1)
        nested_enums = []

        # Find all nested enums
        enum_pattern = r'public\s+enum\s+(\w+)\s*\{([^}]+)\}'
        for match in re.finditer(enum_pattern, content):
            enum_name = match.group(1)
            enum_body = match.group(2)

            values = []
            for line in enum_body.split('\n'):
                line = line.strip()
                if line and not line.startswith('//') and not line.startswith('*'):
                    value = re.sub(r'[,;].*', '', line).strip()
                    if value and value.isupper():
                        values.append(value)

            if values:
                nested_enums.append((parent_class, enum_name, values))

        return nested_enums

    def parse_constants_class(self, file_path: str) -> Dict[str, Dict[str, str]]:
        """Parse Java constants class and return nested structure."""
        with open(file_path, 'r') as f:
            content = f.read()

        constants = {}

        # Handle EntityTypes pattern
        if 'EntityTypes' in file_path:
            values = {}
            for match in re.finditer(r'public\s+static\s+final\s+int\s+(\w+)\s*=\s*(\d+);', content):
                name = match.group(1)
                value = match.group(2)
                values[name] = value
            if values:
                constants['EntityTypes'] = values

        # Handle nested class pattern (like Constants.FormattedResponse)
        elif 'Constants.java' in file_path:
            class_pattern = r'public\s+static\s+class\s+(\w+)\s*\{([^}]+)\}'
            for class_match in re.finditer(class_pattern, content):
                class_name = class_match.group(1)
                class_body = class_match.group(2)

                values = {}
                for const_match in re.finditer(r'public\s+static\s+final\s+String\s+(\w+)\s*=\s*"([^"]+)";', class_body):
                    name = const_match.group(1)
                    value = const_match.group(2)
                    values[name] = f'"{value}"'

                if values:
                    constants[class_name] = values

        return constants

    def generate_typescript_enum(self, enum_name: str, values: List[str]) -> str:
        """Generate TypeScript enum definition."""
        ts_content = f"export enum {enum_name} {{\n"
        for value in values:
            ts_content += f"  {value} = '{value}',\n"
        ts_content += "}\n"
        return ts_content

    def generate_typescript_const(self, const_name: str, values: Dict[str, str]) -> str:
        """Generate TypeScript const object definition."""
        ts_content = f"export const {const_name} = {{\n"
        for key, value in values.items():
            ts_content += f"  {key}: {value},\n"
        ts_content += "} as const;\n\n"

        # Add type definition
        ts_content += f"export type {const_name}Type = typeof {const_name};\n"
        return ts_content

    def export_standalone_enums(self):
        """Export all standalone enums from constants/attendance directory."""
        attendance_constants_path = os.path.join(
            self.backend_path,
            'src/main/java/com/tse/core_application/constants/attendance'
        )

        if not os.path.exists(attendance_constants_path):
            print(f"Warning: Path not found: {attendance_constants_path}")
            return

        for file_name in os.listdir(attendance_constants_path):
            if file_name.endswith('.java'):
                file_path = os.path.join(attendance_constants_path, file_name)
                enum_name, values = self.parse_standalone_enum(file_path)

                if enum_name and values:
                    ts_content = self.generate_typescript_enum(enum_name, values)
                    output_file = os.path.join(self.output_path, f"{enum_name}.ts")

                    with open(output_file, 'w') as f:
                        f.write(ts_content)

                    self.exported_files.append((enum_name, output_file))
                    print(f"✓ Exported {enum_name} → {output_file}")

    def export_nested_enums(self):
        """Export nested enums from entity files."""
        entity_files = [
            'src/main/java/com/tse/core_application/entity/policy/AttendancePolicy.java',
            'src/main/java/com/tse/core_application/entity/fence/GeoFence.java',
            'src/main/java/com/tse/core_application/entity/punch/PunchRequest.java',
        ]

        for entity_file in entity_files:
            file_path = os.path.join(self.backend_path, entity_file)

            if not os.path.exists(file_path):
                print(f"Warning: File not found: {file_path}")
                continue

            nested_enums = self.parse_nested_enum(file_path)

            for parent_class, enum_name, values in nested_enums:
                # Use flattened name for TypeScript
                ts_enum_name = f"{parent_class}{enum_name}"
                ts_content = self.generate_typescript_enum(ts_enum_name, values)
                output_file = os.path.join(self.output_path, f"{ts_enum_name}.ts")

                with open(output_file, 'w') as f:
                    f.write(ts_content)

                self.exported_files.append((ts_enum_name, output_file))
                print(f"✓ Exported {parent_class}.{enum_name} → {ts_enum_name}.ts")

    def export_constants(self):
        """Export constant classes."""
        constant_files = [
            ('src/main/java/com/tse/core_application/constants/EntityTypes.java', 'EntityTypes'),
            ('src/main/java/com/tse/core_application/DummyClasses/Constants.java', 'Constants'),
        ]

        for const_file, label in constant_files:
            file_path = os.path.join(self.backend_path, const_file)

            if not os.path.exists(file_path):
                print(f"Warning: File not found: {file_path}")
                continue

            constants = self.parse_constants_class(file_path)

            for const_name, values in constants.items():
                ts_content = self.generate_typescript_const(const_name, values)
                output_file = os.path.join(self.output_path, f"{const_name}.ts")

                with open(output_file, 'w') as f:
                    f.write(ts_content)

                self.exported_files.append((const_name, output_file))
                print(f"✓ Exported {const_name} constants → {output_file}")

    def generate_index_file(self):
        """Generate index.ts file that exports everything."""
        index_content = "// Auto-generated index file for all exported enums and constants\n\n"

        for name, file_path in sorted(self.exported_files):
            file_name = os.path.basename(file_path).replace('.ts', '')
            index_content += f"export * from './{file_name}';\n"

        index_file = os.path.join(self.output_path, 'index.ts')
        with open(index_file, 'w') as f:
            f.write(index_content)

        print(f"\n✓ Generated index.ts with {len(self.exported_files)} exports")

    def export_all(self):
        """Run all export operations."""
        # Create output directory
        os.makedirs(self.output_path, exist_ok=True)

        print("Starting Java → TypeScript export...\n")

        print("=== Exporting Standalone Enums ===")
        self.export_standalone_enums()

        print("\n=== Exporting Nested Enums ===")
        self.export_nested_enums()

        print("\n=== Exporting Constants ===")
        self.export_constants()

        print("\n=== Generating Index File ===")
        self.generate_index_file()

        print(f"\n✅ Export complete! {len(self.exported_files)} files generated in {self.output_path}")


if __name__ == "__main__":
    # Paths
    backend_path = "/root/Geo-fence/backend"
    output_path = "/root/Geo-fence/enums-export"

    # Run exporter
    exporter = JavaToTypeScriptExporter(backend_path, output_path)
    exporter.export_all()
