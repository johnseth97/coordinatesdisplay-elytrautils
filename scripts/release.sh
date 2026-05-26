#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?usage: ./scripts/release.sh 1.0.2}"
TAG="v$VERSION"

git status --short

if [ -n "$(git status --porcelain)" ]; then
  echo "Working tree is dirty. Commit first."
  exit 1
fi

git fetch origin
git push origin main

echo "Tagging HEAD as $TAG"
git tag -a "$TAG" -m "Release $VERSION"
git push origin "$TAG"
