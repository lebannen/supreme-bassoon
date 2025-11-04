#!/usr/bin/env python3
"""
Build an index for fast lookup of entries in the Wiktionary XML dump.

Creates a SQLite database mapping:
- word title -> byte offset in XML file
- word title -> page ID
- word title -> namespace

This allows instant lookup without parsing the entire file.
"""

import sqlite3
import os
from datetime import datetime

def build_index(xml_path, index_db_path):
    """
    Build index by scanning XML file and recording byte offsets.
    """
    print(f"Building index for: {xml_path}")
    print(f"Output database: {index_db_path}")
    print(f"Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

    # Get file size
    file_size = os.path.getsize(xml_path)
    print(f"File size: {file_size:,} bytes ({file_size / (1024**3):.2f} GB)\n")

    # Create/recreate database
    if os.path.exists(index_db_path):
        os.remove(index_db_path)

    conn = sqlite3.connect(index_db_path)
    cursor = conn.cursor()

    # Create index table
    cursor.execute('''
        CREATE TABLE page_index (
            title TEXT PRIMARY KEY,
            page_id TEXT,
            namespace TEXT,
            byte_offset INTEGER,
            byte_length INTEGER
        )
    ''')

    # Create index for fast lookups
    cursor.execute('CREATE INDEX idx_title ON page_index(title)')

    stats = {
        'total_entries': 0,
        'main_namespace': 0
    }

    last_report = datetime.now()
    commit_batch = 10000

    with open(xml_path, 'rb') as f:
        in_page = False
        page_start_offset = 0
        current_title = None
        current_page_id = None
        current_namespace = None
        buffer = b''

        while True:
            chunk = f.read(1024 * 1024)  # Read 1MB at a time
            if not chunk:
                break

            buffer += chunk

            # Process buffer line by line
            while b'\n' in buffer:
                line_end = buffer.index(b'\n')
                line = buffer[:line_end + 1]
                buffer = buffer[line_end + 1:]

                line_str = line.decode('utf-8', errors='ignore').strip()

                # Detect page start
                if '<page>' in line_str:
                    in_page = True
                    page_start_offset = f.tell() - len(buffer) - len(line)

                # Extract title
                elif in_page and '<title>' in line_str:
                    start = line_str.index('<title>') + 7
                    end = line_str.index('</title>')
                    current_title = line_str[start:end]

                # Extract page ID
                elif in_page and '<id>' in line_str and current_page_id is None:
                    start = line_str.index('<id>') + 4
                    end = line_str.index('</id>')
                    current_page_id = line_str[start:end]

                # Extract namespace
                elif in_page and '<ns>' in line_str:
                    start = line_str.index('<ns>') + 4
                    end = line_str.index('</ns>')
                    current_namespace = line_str[start:end]

                # Detect page end
                elif '</page>' in line_str and in_page:
                    page_end_offset = f.tell() - len(buffer)
                    byte_length = page_end_offset - page_start_offset

                    if current_title:
                        # Insert into database
                        cursor.execute('''
                            INSERT OR REPLACE INTO page_index
                            (title, page_id, namespace, byte_offset, byte_length)
                            VALUES (?, ?, ?, ?, ?)
                        ''', (current_title, current_page_id, current_namespace,
                              page_start_offset, byte_length))

                        stats['total_entries'] += 1
                        if current_namespace == '0':
                            stats['main_namespace'] += 1

                        # Commit periodically
                        if stats['total_entries'] % commit_batch == 0:
                            conn.commit()

                            # Progress report
                            now = datetime.now()
                            if (now - last_report).seconds >= 10:
                                progress = (f.tell() / file_size) * 100
                                print(f"Progress: {progress:.1f}% | "
                                      f"{stats['total_entries']:,} entries indexed | "
                                      f"{stats['main_namespace']:,} dictionary entries")
                                last_report = now

                    # Reset state
                    in_page = False
                    current_title = None
                    current_page_id = None
                    current_namespace = None

    # Final commit
    conn.commit()

    # Create metadata table
    cursor.execute('''
        CREATE TABLE metadata (
            key TEXT PRIMARY KEY,
            value TEXT
        )
    ''')

    cursor.execute('INSERT INTO metadata VALUES (?, ?)',
                   ('xml_path', xml_path))
    cursor.execute('INSERT INTO metadata VALUES (?, ?)',
                   ('indexed_at', datetime.now().isoformat()))
    cursor.execute('INSERT INTO metadata VALUES (?, ?)',
                   ('total_entries', str(stats['total_entries'])))
    cursor.execute('INSERT INTO metadata VALUES (?, ?)',
                   ('main_namespace_entries', str(stats['main_namespace'])))

    conn.commit()
    conn.close()

    # Print summary
    print("\n" + "="*70)
    print("INDEX BUILD COMPLETE")
    print("="*70)
    print(f"Total entries indexed: {stats['total_entries']:,}")
    print(f"Dictionary entries (ns=0): {stats['main_namespace']:,}")
    print(f"Index database: {index_db_path}")

    db_size = os.path.getsize(index_db_path)
    print(f"Index size: {db_size:,} bytes ({db_size / (1024**2):.2f} MB)")

    return stats

if __name__ == "__main__":
    xml_path = "/Users/andrii/Downloads/enwiktionary-latest-pages-articles.xml"
    index_db_path = "/Users/andrii/Projects/vocabee/wiktionary_index.db"

    build_index(xml_path, index_db_path)
