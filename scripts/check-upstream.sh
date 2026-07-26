#!/usr/bin/env bash
# scripts/check-upstream.sh — safe to run anytime; makes zero local changes
set -euo pipefail

REMOTE="${1:-upstream}"

if ! git remote get-url "$REMOTE" >/dev/null 2>&1; then
  echo "Remote '$REMOTE' does not exist."
  echo "Add it with: git remote add $REMOTE https://github.com/<owner>/<repo>.git"
  exit 1
fi

git fetch "$REMOTE" --quiet

BEHIND=$(git rev-list --count HEAD.."$REMOTE/main" 2>/dev/null || echo "0")
if [ "$BEHIND" -eq 0 ]; then
  echo "Up to date with $REMOTE/main."
else
  echo "$BEHIND new commit(s) available on $REMOTE/main:"
  git log HEAD.."$REMOTE/main" --oneline
  echo
  echo "Review with:  git diff HEAD..$REMOTE/main"
  echo "Apply with:   git merge $REMOTE/main   (after you've reviewed it)"
fi
