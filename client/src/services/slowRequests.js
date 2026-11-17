// Tracks requests that have been waiting for the backend for a long time. On a free host the
// backend sleeps when idle and starting it takes minutes, so the interface can tell people why
// they are waiting instead of looking frozen.

const SLOW_AFTER_MS = 8000;
// Safety net: a request whose end is never reported must not keep the notice on screen forever.
const GIVE_UP_AFTER_MS = 5 * 60 * 1000;

let pending = 0;
let slow = false;
const listeners = new Set();

function notify() {
  listeners.forEach((listener) => listener());
}

/** Call when a request starts. Returns a function to call, once, when that request ends. */
export function trackRequest() {
  pending += 1;
  let finished = false;

  const slowTimer = setTimeout(() => {
    if (!slow) {
      slow = true;
      notify();
    }
  }, SLOW_AFTER_MS);

  const finish = () => {
    if (finished) return;
    finished = true;
    clearTimeout(slowTimer);
    clearTimeout(giveUpTimer);
    pending -= 1;
    if (pending === 0 && slow) {
      slow = false;
      notify();
    }
  };
  const giveUpTimer = setTimeout(finish, GIVE_UP_AFTER_MS);

  return finish;
}

export function isRequestSlow() {
  return slow;
}

export function subscribeToSlowRequests(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}