#!/usr/bin/env python3
"""Bangun dataset kurikulum Nihongo dari checkout OpenJLPT.

Hanya memakai pustaka standar Python 3.
"""
import argparse
import datetime
import hashlib
import json
import shutil
import sys
from pathlib import Path

LEVELS = ["N5", "N4", "N3", "N2", "N1"]
STATUSES = {"draft", "reviewed", "verified"}
DEFAULT_STATUS = "draft"
MANIFEST_SCHEMA_VERSION = 1

problems = []
warnings = []


def problem(message):
    problems.append(message)


def warn(message):
    warnings.append(message)


def clean(value):
    return value.strip() if isinstance(value, str) else ""


def str_list(value):
    if isinstance(value, list):
        return [clean(v) for v in value if clean(v)]
    if isinstance(value, str) and value.strip():
        return [value.strip()]
    return []


def load_items(path):
    with path.open(encoding="utf-8") as handle:
        raw = json.load(handle)
    if isinstance(raw, list):
        return raw
    if isinstance(raw, dict):
        for key in ("items", "data", "entries"):
            if isinstance(raw.get(key), list):
                return raw[key]
        for value in raw.values():
            if isinstance(value, list):
                return value
    raise SystemExit(f"Format file sumber tidak dikenali: {path}")


def norm_examples(value, with_furigana, limit):
    result = []
    for example in value if isinstance(value, list) else []:
        if not isinstance(example, dict):
            continue
        ja = clean(example.get("ja"))
        if not ja:
            continue
        item = {"ja": ja}
        en = clean(example.get("en"))
        if en:
            item["en"] = en
        furigana = clean(example.get("furigana"))
        if with_furigana and furigana:
            item["furigana"] = furigana
        result.append(item)
    return result[:limit]


def check_level(raw, level, label):
    raw_level = clean(raw.get("level"))
    if raw_level and raw_level != level:
        problem(f"{label}: level di data ({raw_level}) berbeda dengan file ({level})")


def normalize_vocab(raw, level):
    item_id = clean(raw.get("id"))
    word = clean(raw.get("word"))
    meanings = str_list(raw.get("meanings"))
    if not item_id or not word or not meanings:
        problem(f"kosakata {level}: id/word/meanings kosong -> {raw!r:.120}")
        return None
    check_level(raw, level, f"kosakata {item_id}")
    item = {
        "id": item_id,
        "word": word,
        "reading": clean(raw.get("reading")) or word,
        "romaji": clean(raw.get("romaji")),
        "level": level,
        "meaningsEn": meanings,
        "pos": str_list(raw.get("pos")),
        "examples": norm_examples(raw.get("examples"), True, 2),
    }
    if isinstance(raw.get("jmdict_id"), int):
        item["jmdictId"] = raw["jmdict_id"]
    return item


def normalize_kanji(raw, level):
    character = clean(raw.get("character"))
    strokes = raw.get("strokes")
    if not character or not isinstance(strokes, int):
        problem(f"kanji {level}: character/strokes tidak valid -> {raw!r:.120}")
        return None
    check_level(raw, level, f"kanji {character}")
    item = {
        "id": character,
        "character": character,
        "level": level,
        "strokes": strokes,
        "onyomi": str_list(raw.get("onyomi")),
        "kunyomi": str_list(raw.get("kunyomi")),
        "meaningsEn": str_list(raw.get("meanings")),
        "words": str_list(raw.get("words")),
    }
    for key in ("grade", "freq"):
        if isinstance(raw.get(key), int):
            item[key] = raw[key]
    radical = clean(raw.get("radical"))
    if radical:
        item["radical"] = radical
    return item


def normalize_grammar(raw, level):
    item_id = clean(raw.get("id"))
    pattern = clean(raw.get("pattern"))
    meaning = clean(raw.get("meaning"))
    if not item_id or not pattern or not meaning:
        problem(f"grammar {level}: id/pattern/meaning kosong -> {raw!r:.120}")
        return None
    check_level(raw, level, f"grammar {item_id}")
    item = {
        "id": item_id,
        "pattern": pattern,
        "level": level,
        "meaningEn": meaning,
        "formation": clean(raw.get("formation")),
        "tags": str_list(raw.get("tags")),
        "examples": norm_examples(raw.get("examples"), False, 3),
    }
    for src_key, dst_key in (("reading", "reading"), ("romaji", "romaji"), ("notes", "notes")):
        value = clean(raw.get(src_key))
        if value:
            item[dst_key] = value
    return item


TYPES = [
    # (tipe di manifest, folder sumber & keluaran, kunci di meta.json, fungsi normalisasi)
    ("vocabulary", "vocab", "vocab", normalize_vocab),
    ("kanji", "kanji", "kanji", normalize_kanji),
    ("grammar", "grammar", "grammar", normalize_grammar),
]


def load_overlay(trans_dir, mtype):
    """Gabungkan semua file '<tipe>*.json' di folder terjemahan.

    Nilai boleh berupa teks (otomatis berstatus draft) atau objek
    {"meaningId": "...", "status": "draft|reviewed|verified"}.
    """
    merged = {}
    for path in sorted(trans_dir.glob(f"{mtype}*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(data, dict):
            problem(f"{path}: overlay terjemahan harus berupa objek JSON")
            continue
        for key, value in data.items():
            if key in merged:
                problem(f"{path.name}: ID '{key}' muncul lebih dari sekali di file overlay")
            merged[key] = value
    return merged


def parse_translation(entry):
    if isinstance(entry, str):
        return clean(entry), DEFAULT_STATUS
    if isinstance(entry, dict):
        return clean(entry.get("meaningId")), entry.get("status", DEFAULT_STATUS)
    return "", None


def apply_translation(item, overlay):
    meaning_id, status = parse_translation(overlay.get(item["id"]))
    if meaning_id and status in STATUSES:
        item["meaningId"] = meaning_id
        item["translationStatus"] = status
    else:
        item["translationStatus"] = "none"


def write_json(path, obj):
    data = json.dumps(obj, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    return data


def todo_line(mtype, item):
    if mtype == "vocabulary":
        return "\t".join([item["id"], item["word"], item["reading"], "; ".join(item["meaningsEn"])])
    if mtype == "kanji":
        return "\t".join([item["id"], item["character"], "; ".join(item["meaningsEn"])])
    return "\t".join([item["id"], item["pattern"], item["meaningEn"]])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--translations", required=True)
    parser.add_argument("--audit", required=True)
    parser.add_argument("--todo-dir", default="")
    parser.add_argument("--dataset-version", type=int, required=True)
    parser.add_argument("--source-commit", default="")
    args = parser.parse_args()

    src = Path(args.source)
    out = Path(args.out)
    trans_dir = Path(args.translations)
    meta_path = src / "data" / "json" / "meta.json"
    if not meta_path.exists():
        raise SystemExit(f"meta.json tidak ditemukan: {meta_path}")
    meta = json.loads(meta_path.read_text(encoding="utf-8"))
    upstream_counts = meta.get("counts", {})

    for folder in ("vocab", "kanji", "grammar"):
        shutil.rmtree(out / folder, ignore_errors=True)
    manifest_path = out / "manifest.json"
    if manifest_path.exists():
        manifest_path.unlink()
    todo_dir = Path(args.todo_dir) if args.todo_dir else None
    if todo_dir:
        shutil.rmtree(todo_dir, ignore_errors=True)
        todo_dir.mkdir(parents=True, exist_ok=True)

    files = []
    totals = {}
    rows = []  # untuk laporan audit
    coverage = {}
    coverage_by_level = {}
    extra = {"vocab_with_examples": 0}

    for mtype, folder, meta_key, normalizer in TYPES:
        overlay = load_overlay(trans_dir, mtype)
        seen = {}
        translated = 0
        total = 0
        for level in LEVELS:
            src_file = src / "data" / "json" / folder / f"{level.lower()}.json"
            if not src_file.exists():
                problem(f"File sumber tidak ada: {src_file}")
                continue
            items = []
            level_translated = 0
            for raw in load_items(src_file):
                if not isinstance(raw, dict):
                    problem(f"{mtype} {level}: entri bukan objek")
                    continue
                item = normalizer(raw, level)
                if item is None:
                    continue
                if item["id"] in seen:
                    problem(f"{mtype}: ID duplikat '{item['id']}' ({seen[item['id']]} dan {level})")
                    continue
                seen[item["id"]] = level
                apply_translation(item, overlay)
                if item["translationStatus"] != "none":
                    translated += 1
                    level_translated += 1
                if mtype == "vocabulary" and item["examples"]:
                    extra["vocab_with_examples"] += 1
                items.append(item)
            expected = upstream_counts.get(meta_key, {}).get(level)
            if expected is not None and expected != len(items):
                problem(f"{mtype} {level}: hasil {len(items)} berbeda dengan meta.json upstream {expected}")
            relative = f"{folder}/{level.lower()}.json"
            data = write_json(out / relative, {"items": items})
            files.append({
                "path": relative,
                "type": mtype,
                "level": level,
                "count": len(items),
                "sha256": hashlib.sha256(data).hexdigest(),
            })
            rows.append((mtype, level, len(items), expected, len(data)))
            coverage_by_level[(mtype, level)] = (level_translated, len(items))
            total += len(items)
            if todo_dir:
                pending = [todo_line(mtype, i) for i in items if i["translationStatus"] == "none"]
                (todo_dir / f"{mtype}-{level.lower()}.tsv").write_text("\n".join(pending) + "\n", encoding="utf-8")
        orphans = [key for key in overlay if key not in seen]
        if orphans:
            warn(f"{mtype}: {len(orphans)} ID di overlay terjemahan tidak ada di dataset: {orphans[:30]}")
        invalid = [key for key, value in overlay.items() if key in seen and parse_translation(value)[1] not in STATUSES]
        if invalid:
            warn(f"{mtype}: {len(invalid)} entri overlay punya status tidak valid dan diabaikan: {invalid[:30]}")
        totals[mtype] = total
        coverage[mtype] = (translated, total)

    audit = ["# Audit Dataset Kurikulum", "",
             f"- Versi dataset: {args.dataset_version}",
             f"- Sumber: OpenJLPT {meta.get('version', '?')} (commit `{args.source_commit[:12]}`)",
             f"- Lisensi sumber: {meta.get('license', '?')}", "",
             "| Tipe | Level | Jumlah hasil | Jumlah upstream | Cocok | Ukuran (KB) |",
             "|---|---|---|---|---|---|"]
    for mtype, level, count, expected, size in rows:
        match = "-" if expected is None else ("ya" if expected == count else "TIDAK")
        audit.append(f"| {mtype} | {level} | {count} | {expected if expected is not None else '-'} | {match} | {size // 1024} |")
    audit += ["", "## Total", ""]
    for mtype, total in totals.items():
        audit.append(f"- {mtype}: {total}")
    audit += ["", f"- Kosakata dengan kalimat contoh: {extra['vocab_with_examples']}", "",
              "## Cakupan terjemahan Indonesia", ""]
    for mtype, (done, total) in coverage.items():
        percent = (100.0 * done / total) if total else 0.0
        audit.append(f"- {mtype}: {done} dari {total} ({percent:.1f}%)")
    audit += ["", "| Tipe | Level | Diterjemahkan | Total |", "|---|---|---|---|"]
    for (mtype, level), (done, total) in coverage_by_level.items():
        audit.append(f"| {mtype} | {level} | {done} | {total} |")
    audit += ["", "## Peringatan", ""]
    audit += [f"- {w}" for w in warnings] if warnings else ["- Tidak ada."]
    audit += ["", "## Masalah", ""]
    audit += [f"- {p}" for p in problems] if problems else ["- Tidak ada."]
    audit.append("")
    Path(args.audit).parent.mkdir(parents=True, exist_ok=True)
    Path(args.audit).write_text("\n".join(audit), encoding="utf-8")
    print("\n".join(audit))

    if problems:
        print(f"\nGAGAL: {len(problems)} masalah ditemukan. Manifest tidak ditulis.", file=sys.stderr)
        sys.exit(1)

    manifest = {
        "schemaVersion": MANIFEST_SCHEMA_VERSION,
        "datasetVersion": args.dataset_version,
        "generatedAt": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "source": {
            "name": "OpenJLPT",
            "version": str(meta.get("version", "")),
            "commit": args.source_commit,
            "license": str(meta.get("license", "")),
            "url": str(meta.get("homepage", "https://github.com/evanclan/OpenJLPT")),
        },
        "counts": totals,
        "files": files,
    }
    write_json(manifest_path, manifest)
    print(f"Selesai. {len(files)} file ditulis ke {out}")


if __name__ == "__main__":
    main()