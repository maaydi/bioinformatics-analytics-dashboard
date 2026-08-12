#!/usr/bin/env python3

import os
import re
from pathlib import Path

# Root directory containing your sources
SOURCE_ROOT = Path("/home/medali/VscodeProjects/bioinformatics-analytics-dashboard/documentation")


def dynamic_merge_md(output_filename="merged_output.md"):
    root_files = []
    subfolder_files = []

    # Scan the current directory
    for root, dirs, files in os.walk(SOURCE_ROOT):
        # Filter for markdown files
        md_files = [os.path.join(root, f) for f in files if f.endswith(".md") and not f.startswith("journal")]

        # Skip the output file itself if it already exists
        md_files = [f for f in md_files if os.path.basename(f) != output_filename]

        if root == SOURCE_ROOT:
            # Files sitting directly in the root folder
            root_files.extend(sorted(md_files))
        else:
            # Files sitting inside subfolders
            subfolder_files.extend(sorted(md_files))

    # Combine lists: root files first, then subfolder files
    all_files = root_files + subfolder_files

    # Merge everything into the output file
    with open(output_filename, "w", encoding="utf-8") as outfile:
        for file_path in all_files:
            with open(file_path, "r", encoding="utf-8") as infile:
                # Optional: Adds a header so you know where the content came from
                outfile.write(f"<!-- Start of {file_path} -->\n\n")
                outfile.write(infile.read())
                outfile.write("\n\n")
            print(f"Successfully merged: {file_path}")


if __name__ == "__main__":
    dynamic_merge_md()
