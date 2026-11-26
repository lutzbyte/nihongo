#!/usr/bin/env python3
"""Bangun materi Foundation (kana dan pelajaran dasar) dan tambahkan ke manifest.

Dijalankan SETELAH build_curriculum.py. Hanya memakai pustaka standar Python 3.
"""
import argparse
import hashlib
import json
import sys
from pathlib import Path

# (baris, daftar kana, romaji dipisah spasi)
BASIC = [
    ("a", list("あいうえお"), "a i u e o"),
    ("ka", list("かきくけこ"), "ka ki ku ke ko"),
    ("sa", list("さしすせそ"), "sa shi su se so"),
    ("ta", list("たちつてと"), "ta chi tsu te to"),
    ("na", list("なにぬねの"), "na ni nu ne no"),
    ("ha", list("はひふへほ"), "ha hi fu he ho"),
    ("ma", list("まみむめも"), "ma mi mu me mo"),
    ("ya", list("やゆよ"), "ya yu yo"),
    ("ra", list("らりるれろ"), "ra ri ru re ro"),
    ("wa", list("わを"), "wa wo"),
    ("n", list("ん"), "n"),
]
DAKUTEN = [
    ("ga", list("がぎぐげご"), "ga gi gu ge go"),
    ("za", list("ざじずぜぞ"), "za ji zu ze zo"),
    ("da", list("だぢづでど"), "da ji zu de do"),
    ("ba", list("ばびぶべぼ"), "ba bi bu be bo"),
]
HANDAKUTEN = [
    ("pa", list("ぱぴぷぺぽ"), "pa pi pu pe po"),
]
YOON = [
    ("kya", ["きゃ", "きゅ", "きょ"], "kya kyu kyo"),
    ("sha", ["しゃ", "しゅ", "しょ"], "sha shu sho"),
    ("cha", ["ちゃ", "ちゅ", "ちょ"], "cha chu cho"),
    ("nya", ["にゃ", "にゅ", "にょ"], "nya nyu nyo"),
    ("hya", ["ひゃ", "ひゅ", "ひょ"], "hya hyu hyo"),
    ("mya", ["みゃ", "みゅ", "みょ"], "mya myu myo"),
    ("rya", ["りゃ", "りゅ", "りょ"], "rya ryu ryo"),
    ("gya", ["ぎゃ", "ぎゅ", "ぎょ"], "gya gyu gyo"),
    ("ja", ["じゃ", "じゅ", "じょ"], "ja ju jo"),
    ("bya", ["びゃ", "びゅ", "びょ"], "bya byu byo"),
    ("pya", ["ぴゃ", "ぴゅ", "ぴょ"], "pya pyu pyo"),
]
GROUPS = [("basic", BASIC), ("dakuten", DAKUTEN), ("handakuten", HANDAKUTEN), ("yoon", YOON)]

problems = []


def to_katakana(text):
    return "".join(chr(ord(c) + 0x60) if "ぁ" <= c <= "ゖ" else c for c in text)


def build_kana():
    items = []
    order = 0
    for script in ("hiragana", "katakana"):
        for group, rows in GROUPS:
            for row, kanas, romaji_text in rows:
                romajis = romaji_text.split()
                if len(romajis) != len(kanas):
                    problems.append(f"baris {row}: jumlah kana dan romaji tidak sama")
                    continue
                for kana, romaji in zip(kanas, romajis):
                    order += 1
                    value = kana if script == "hiragana" else to_katakana(kana)
                    items.append({
                        "id": f"{script}:{value}",
                        "script": script,
                        "kana": value,
                        "romaji": romaji,
                        "group": group,
                        "row": row,
                        "order": order,
                    })
    ids = [i["id"] for i in items]
    if len(ids) != len(set(ids)):
        problems.append("ID kana duplikat")
    return items


def build_lessons(path):
    raw = json.loads(path.read_text(encoding="utf-8"))
    items = raw.get("items") if isinstance(raw, dict) else None
    if not isinstance(items, list):
        problems.append(f"{path}: harus berisi objek dengan daftar 'items'")
        return []
    seen = set()
    for lesson in items:
        lid = lesson.get("id", "")
        if not lid or lid in seen:
            problems.append(f"pelajaran: id kosong atau duplikat '{lid}'")
        seen.add(lid)
        if not lesson.get("title"):
            problems.append(f"pelajaran {lid}: title kosong")
        if not isinstance(lesson.get("order"), int):
            problems.append(f"pelajaran {lid}: order harus bilangan bulat")
        sections = lesson.get("sections")
        if not isinstance(sections, list) or not sections:
            problems.append(f"pelajaran {lid}: sections kosong")
            continue
        for section in sections:
            if not section.get("heading"):
                problems.append(f"pelajaran {lid}: ada section tanpa heading")
            for row in section.get("rows", []):
                if not row.get("ja") or not row.get("meaning"):
                    problems.append(f"pelajaran {lid}: baris tanpa ja/meaning di '{section.get('heading')}'")
    return items


def write_json(path, obj):
    data = json.dumps(obj, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    return data


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", default="dataset/foundation")
    parser.add_argument("--out", required=True)
    parser.add_argument("--audit", required=True)
    args = parser.parse_args()

    out = Path(args.out)
    manifest_path = out / "manifest.json"
    if not manifest_path.exists():
        raise SystemExit("manifest.json belum ada. Jalankan build_curriculum.py lebih dulu.")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))

    kana = build_kana()
    lessons = build_lessons(Path(args.source) / "lessons.json")

    audit_lines = ["", "## Foundation", "",
                   f"- Kana: {len(kana)} (hiragana dan katakana, 104 masing-masing diharapkan)",
                   f"- Pelajaran: {len(lessons)}"]
    if len(kana) != 208:
        problems.append(f"jumlah kana {len(kana)} bukan 208")
    audit_lines += ["", "### Masalah Foundation", ""]
    audit_lines += [f"- {p}" for p in problems] if problems else ["- Tidak ada."]
    audit_lines.append("")
    with open(args.audit, "a", encoding="utf-8") as handle:
        handle.write("\n".join(audit_lines))
    print("\n".join(audit_lines))

    if problems:
        print(f"\nGAGAL: {len(problems)} masalah pada materi Foundation.", file=sys.stderr)
        sys.exit(1)

    files = [f for f in manifest.get("files", []) if f.get("type") not in ("kana", "lesson")]
    for relative, mtype, items in (("foundation/kana.json", "kana", kana),
                                   ("foundation/lessons.json", "lesson", lessons)):
        data = write_json(out / relative, {"items": items})
        files.append({
            "path": relative,
            "type": mtype,
            "level": "-",
            "count": len(items),
            "sha256": hashlib.sha256(data).hexdigest(),
        })
    manifest["files"] = files
    counts = manifest.setdefault("counts", {})
    counts["kana"] = len(kana)
    counts["lessons"] = len(lessons)
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"Foundation selesai: {len(kana)} kana, {len(lessons)} pelajaran.")


if __name__ == "__main__":
    main()