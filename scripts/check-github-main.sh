#!/usr/bin/env bash
set -euo pipefail

die() {
  printf 'GitHub main check: %s\n' "$1" >&2
  exit 2
}

repo_root=$(git rev-parse --show-toplevel 2>/dev/null) ||
  die "run this from inside the Git repository."

local_commit=$(git -C "$repo_root" rev-parse --verify 'refs/heads/main^{commit}' 2>/dev/null) ||
  die "local main is unavailable."
local_tree=$(git -C "$repo_root" rev-parse --verify "${local_commit}^{tree}" 2>/dev/null) ||
  die "could not read the local main tree."

read_remote_main() {
  local result
  result=$(GIT_TERMINAL_PROMPT=0 git -C "$repo_root" ls-remote --exit-code origin refs/heads/main 2>/dev/null) ||
    return 1
  read -r REPLY REMOTE_REF <<< "$result"
  [[ "$REMOTE_REF" == refs/heads/main && "$REPLY" =~ ^[0-9a-f]{40,64}$ ]]
}

read_remote_main || die "could not read the live origin main branch."
remote_before=$REPLY
fetch_url=$(git -C "$repo_root" remote get-url origin 2>/dev/null) ||
  die "could not read the origin fetch address."

tmpdir=$(mktemp -d "${TMPDIR:-/tmp}/github-main-check.XXXXXX") ||
  die "could not create a temporary history store."
trap 'rm -rf "$tmpdir"' EXIT
local_objects=$(git -C "$repo_root" rev-parse --path-format=absolute --git-path objects 2>/dev/null) ||
  die "could not locate the local Git object store."
object_format=$(git -C "$repo_root" rev-parse --show-object-format 2>/dev/null) ||
  die "could not determine the local Git object format."
analysis_dir="$tmpdir/analysis.git"
git init --bare -q --object-format="$object_format" "$analysis_dir" 2>/dev/null ||
  die "could not create a temporary history checker."

# Fetch complete remote history into a separate temporary repository. The
# blob filter avoids downloading file contents; local refs are never updated.
if ! GIT_TERMINAL_PROMPT=0 \
  git --git-dir="$analysis_dir" fetch --quiet --no-tags --no-recurse-submodules \
    --filter=blob:none "$fetch_url" refs/heads/main:refs/check/github-main 2>/dev/null; then
  die "could not fetch GitHub main for comparison."
fi

read_remote_main || die "could not confirm the live origin main branch."
remote_after=$REPLY
remote_commit=$(
  git --git-dir="$analysis_dir" rev-parse --verify 'refs/check/github-main^{commit}' 2>/dev/null
) || die "could not read the fetched GitHub main commit."
[[ "$remote_before" == "$remote_commit" && "$remote_commit" == "$remote_after" ]] ||
  die "GitHub main changed during the check; rerun it."

remote_tree=$(
  git --git-dir="$analysis_dir" rev-parse --verify "${remote_commit}^{tree}" 2>/dev/null
) || die "could not read the live GitHub main tree."

printf 'Local main commit:  %s\n' "$local_commit"
printf 'GitHub main commit: %s\n' "$remote_commit"
printf 'Local main tree:    %s\n' "$local_tree"
printf 'GitHub main tree:   %s\n' "$remote_tree"

declare -A visited_commits=()
pending_commits=("$local_commit")
history_incomplete=false
remote_is_ancestor=false

# Walk parents from local main and stop at the live commit if it is found.
# This can prove ancestry in a shallow checkout when the remote commit itself
# is present, without requiring that commit's older parents.
while ((${#pending_commits[@]})); do
  current_commit=${pending_commits[0]}
  pending_commits=("${pending_commits[@]:1}")
  [[ -z "${visited_commits[$current_commit]+seen}" ]] || continue
  visited_commits["$current_commit"]=1

  if [[ "$current_commit" == "$remote_commit" ]]; then
    remote_is_ancestor=true
    break
  fi

  if ! raw_commit=$(
    GIT_ALTERNATE_OBJECT_DIRECTORIES="$local_objects" \
      git --git-dir="$analysis_dir" cat-file -p "$current_commit" 2>/dev/null
  ); then
    history_incomplete=true
    continue
  fi

  parents=$(printf '%s\n' "$raw_commit" | awk 'NF == 0 { exit } $1 == "parent" { print $2 }')
  for parent_commit in $parents; do
    if [[ "$parent_commit" == "$remote_commit" ]]; then
      remote_is_ancestor=true
      break 2
    fi
    pending_commits+=("$parent_commit")
  done
done

if [[ "$remote_is_ancestor" == true ]]; then
  printf 'History check: OK — GitHub main is an ancestor of local main.\n'
  exit 0
fi

[[ "$history_incomplete" == false ]] ||
  die "history objects are incomplete, so GitHub ancestry could not be confirmed."

printf 'History check: GitHub main is not an ancestor of local main.\n' >&2
printf 'Reconcile the branches before pushing; do not force-push.\n' >&2
exit 1