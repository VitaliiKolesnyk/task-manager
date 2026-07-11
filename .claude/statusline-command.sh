#!/bin/bash
# Claude Code status line: model | dir/branch | context bar+% | cost | time.
# Parses the JSON from stdin with grep/sed (no jq needed) so it works in the
# bundled Git Bash on Windows.
input=$(cat)

# --- helpers: first match of a JSON string / number value by key ----------
jstr() { printf '%s' "$input" | grep -oE "\"$1\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" | head -n1 | sed -E "s/^\"$1\"[[:space:]]*:[[:space:]]*\"//; s/\"$//"; }
jnum() { printf '%s' "$input" | grep -oE "\"$1\"[[:space:]]*:[[:space:]]*[0-9.eE+-]+" | head -n1 | grep -oE "[0-9.eE+-]+$"; }

MODEL=$(jstr display_name); [ -z "$MODEL" ] && MODEL="Claude"
DIR=$(jstr current_dir)
COST=$(jnum total_cost_usd); [ -z "$COST" ] && COST=0
PCT=$(jnum used_percentage); PCT=${PCT%%.*}; [ -z "$PCT" ] && PCT=0
DURATION_MS=$(jnum total_duration_ms); [ -z "$DURATION_MS" ] && DURATION_MS=0

# Basename of the working dir (handles Windows backslash paths).
DIR_SLASH="${DIR//\\//}"; DIR_NAME="${DIR_SLASH##*/}"

CYAN='\033[36m'; GREEN='\033[32m'; YELLOW='\033[33m'; RED='\033[31m'; RESET='\033[0m'

# Context-usage bar (color by fill level).
if [ "$PCT" -ge 90 ]; then BAR_COLOR="$RED"
elif [ "$PCT" -ge 70 ]; then BAR_COLOR="$YELLOW"
else BAR_COLOR="$GREEN"; fi

FILLED=$((PCT / 10)); [ "$FILLED" -gt 10 ] && FILLED=10; EMPTY=$((10 - FILLED))
printf -v FILL "%${FILLED}s"; printf -v PAD "%${EMPTY}s"
BAR="${FILL// /█}${PAD// /░}"

# Session elapsed time from total_duration_ms.
TOTAL_S=$((DURATION_MS / 1000)); H=$((TOTAL_S / 3600)); M=$(((TOTAL_S % 3600) / 60)); S=$((TOTAL_S % 60))
if [ "$H" -gt 0 ]; then TIME="${H}h ${M}m"; else TIME="${M}m ${S}s"; fi

BRANCH=""
git rev-parse --git-dir > /dev/null 2>&1 && BRANCH=" | 🌿 $(git branch --show-current 2>/dev/null)"

COST_FMT=$(printf '$%.4f' "$COST")

printf "${CYAN}[%s]${RESET} 📁 %s%b\n" "$MODEL" "$DIR_NAME" "$BRANCH"
printf "${BAR_COLOR}%s${RESET} %s%% | ${YELLOW}%s${RESET} | ⏱️ %s" "$BAR" "$PCT" "$COST_FMT" "$TIME"
