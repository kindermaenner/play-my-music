import json
import re
from pathlib import Path


STATE_PATTERN = re.compile(r"STATE_REACHED:\s*([A-Za-z0-9_\-]+)")


def load_states(file_path: Path) -> list[str]:
    data = json.loads(file_path.read_text(encoding="utf-8"))
    return list(dict.fromkeys(data.get("states", [])))


def load_reached_states(file_path: Path) -> list[str]:
    reached = []
    for line in file_path.read_text(encoding="utf-8").splitlines():
        match = STATE_PATTERN.search(line)
        if match:
            reached.append(match.group(1))
    return list(dict.fromkeys(reached))


def main() -> None:
    root = Path(__file__).resolve().parent
    states_file = root / "ui-states.json"
    log_file = root / "ui-test-log.txt"
    output_file = root / "ui-flow-coverage.json"

    states = load_states(states_file)
    reached = load_reached_states(log_file)

    covered = sorted(set(states).intersection(reached))
    coverage = 0.0 if not states else round(len(covered) / len(states) * 100, 2)

    result = {
        "states_total": len(states),
        "states_covered": len(covered),
        "coverage": coverage,
        "states": states,
        "covered_states": covered,
    }

    output_file.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()