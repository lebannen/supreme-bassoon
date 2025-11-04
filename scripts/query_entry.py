#!/usr/bin/env python3
"""
Query tool for fast lookup of Wiktionary entries using the index.

Usage:
    python query_entry.py <word>
    python query_entry.py --interactive
    python query_entry.py --search <pattern>
"""

import sqlite3
import sys
import os

class WiktionaryQuery:
    def __init__(self, index_db_path, xml_path):
        if not os.path.exists(index_db_path):
            print(f"Error: Index database not found: {index_db_path}")
            print("Run build_index.py first to create the index.")
            sys.exit(1)

        if not os.path.exists(xml_path):
            print(f"Error: XML file not found: {xml_path}")
            sys.exit(1)

        self.index_db_path = index_db_path
        self.xml_path = xml_path
        self.conn = sqlite3.connect(index_db_path)
        self.cursor = self.conn.cursor()

    def get_entry(self, title):
        """
        Retrieve raw XML for a specific entry.
        """
        self.cursor.execute('''
            SELECT page_id, namespace, byte_offset, byte_length
            FROM page_index
            WHERE title = ?
        ''', (title,))

        result = self.cursor.fetchone()

        if not result:
            return None

        page_id, namespace, byte_offset, byte_length = result

        # Read from XML file at specific offset
        with open(self.xml_path, 'rb') as f:
            f.seek(byte_offset)
            xml_content = f.read(byte_length)

        return {
            'title': title,
            'page_id': page_id,
            'namespace': namespace,
            'xml': xml_content.decode('utf-8', errors='ignore')
        }

    def search_titles(self, pattern, limit=20):
        """
        Search for titles matching a pattern (SQL LIKE).
        """
        self.cursor.execute('''
            SELECT title, page_id, namespace
            FROM page_index
            WHERE title LIKE ?
            ORDER BY title
            LIMIT ?
        ''', (pattern, limit))

        return self.cursor.fetchall()

    def get_stats(self):
        """
        Get index statistics.
        """
        self.cursor.execute('SELECT * FROM metadata')
        metadata = dict(self.cursor.fetchall())

        self.cursor.execute('SELECT COUNT(*) FROM page_index')
        total = self.cursor.fetchone()[0]

        self.cursor.execute('SELECT COUNT(*) FROM page_index WHERE namespace = "0"')
        main_ns = self.cursor.fetchone()[0]

        return {
            'total_entries': total,
            'dictionary_entries': main_ns,
            'indexed_at': metadata.get('indexed_at', 'unknown'),
            'xml_path': metadata.get('xml_path', 'unknown')
        }

    def interactive(self):
        """
        Interactive query mode.
        """
        print("="*70)
        print("WIKTIONARY QUERY TOOL - Interactive Mode")
        print("="*70)

        stats = self.get_stats()
        print(f"Index loaded: {stats['total_entries']:,} entries")
        print(f"Dictionary entries: {stats['dictionary_entries']:,}")
        print(f"Indexed at: {stats['indexed_at']}")
        print("\nCommands:")
        print("  <word>       - Look up exact word")
        print("  /search <pattern> - Search for pattern (use % as wildcard)")
        print("  /stats       - Show statistics")
        print("  /quit        - Exit")
        print("="*70)

        while True:
            try:
                query = input("\n> ").strip()

                if not query:
                    continue

                if query == '/quit':
                    break

                if query == '/stats':
                    stats = self.get_stats()
                    print(f"\nTotal entries: {stats['total_entries']:,}")
                    print(f"Dictionary entries: {stats['dictionary_entries']:,}")
                    print(f"Indexed at: {stats['indexed_at']}")
                    continue

                if query.startswith('/search '):
                    pattern = query[8:].strip()
                    if '%' not in pattern:
                        pattern = f'%{pattern}%'

                    results = self.search_titles(pattern)

                    if not results:
                        print(f"No matches found for: {pattern}")
                    else:
                        print(f"\nFound {len(results)} matches:")
                        for title, page_id, ns in results:
                            ns_label = "dict" if ns == "0" else f"ns:{ns}"
                            print(f"  - {title} (ID: {page_id}, {ns_label})")

                else:
                    # Direct lookup
                    entry = self.get_entry(query)

                    if not entry:
                        print(f"Entry not found: {query}")
                        print("Try /search command with wildcards")
                    else:
                        print(f"\n{'='*70}")
                        print(f"Entry: {entry['title']}")
                        print(f"Page ID: {entry['page_id']}")
                        print(f"Namespace: {entry['namespace']}")
                        print(f"{'='*70}")
                        print(entry['xml'])

            except KeyboardInterrupt:
                print("\n\nExiting...")
                break
            except Exception as e:
                print(f"Error: {e}")

    def close(self):
        self.conn.close()


def main():
    index_db_path = "/Users/andrii/Projects/vocabee/wiktionary_index.db"
    xml_path = "/Users/andrii/Downloads/enwiktionary-latest-pages-articles.xml"

    query_tool = WiktionaryQuery(index_db_path, xml_path)

    if len(sys.argv) == 1:
        print("Usage:")
        print("  python query_entry.py <word>")
        print("  python query_entry.py --interactive")
        print("  python query_entry.py --search <pattern>")
        sys.exit(1)

    if sys.argv[1] == '--interactive':
        query_tool.interactive()

    elif sys.argv[1] == '--search':
        if len(sys.argv) < 3:
            print("Error: --search requires a pattern")
            sys.exit(1)

        pattern = sys.argv[2]
        if '%' not in pattern:
            pattern = f'%{pattern}%'

        results = query_tool.search_titles(pattern, limit=50)

        if not results:
            print(f"No matches found for: {pattern}")
        else:
            print(f"Found {len(results)} matches:\n")
            for title, page_id, ns in results:
                ns_label = "dict" if ns == "0" else f"ns:{ns}"
                print(f"  {title} (ID: {page_id}, {ns_label})")

    else:
        # Direct lookup
        word = sys.argv[1]
        entry = query_tool.get_entry(word)

        if not entry:
            print(f"Entry not found: {word}")
            print("\nTry searching with pattern:")
            print(f"  python query_entry.py --search '{word}%'")
            sys.exit(1)

        print(f"Entry: {entry['title']}")
        print(f"Page ID: {entry['page_id']}")
        print(f"Namespace: {entry['namespace']}")
        print(f"\n{'='*70}")
        print("XML CONTENT:")
        print(f"{'='*70}\n")
        print(entry['xml'])

    query_tool.close()


if __name__ == "__main__":
    main()
