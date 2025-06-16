#!/bin/sh

# Usage : wait-for.sh host:port [-t timeout] [-- command args]

TIMEOUT=15
QUIET=0
HOST=""
PORT=""

usage() {
  echo "Usage: $0 host:port [-t timeout] [-- command args]"
  echo "  -t TIMEOUT   Timeout in seconds (default: 15, 0 = no timeout)"
  echo "  --quiet      Suppress output"
  echo "  --           Execute command after wait"
  exit 1
}

wait_for() {
  [ "$QUIET" -ne 1 ] && echo "⌛ Attente de $HOST:$PORT (timeout=${TIMEOUT}s)..."
  start=$(date +%s)

  while true; do
    if nc -z "$HOST" "$PORT" 2>/dev/null; then
      [ "$QUIET" -ne 1 ] && echo "✅ $HOST:$PORT est disponible !"
      return 0
    fi

    sleep 1

    if [ "$TIMEOUT" -gt 0 ]; then
      now=$(date +%s)
      elapsed=$((now - start))
      if [ "$elapsed" -ge "$TIMEOUT" ]; then
        echo "❌ Timeout après ${TIMEOUT}s en attente de $HOST:$PORT"
        return 1
      fi
    fi
  done
}

# --- Parsing arguments ---
while [ $# -gt 0 ]; do
  case "$1" in
    *:*)
      HOST=$(echo "$1" | cut -d: -f1)
      PORT=$(echo "$1" | cut -d: -f2)
      shift
      ;;
    -t)
      TIMEOUT=$2
      shift 2
      ;;
    --quiet)
      QUIET=1
      shift
      ;;
    --)
      shift
      break
      ;;
    *)
      usage
      ;;
  esac
done

[ -z "$HOST" ] || [ -z "$PORT" ] && usage

wait_for
RESULT=$?

[ $# -gt 0 ] && exec "$@"

exit $RESULT
