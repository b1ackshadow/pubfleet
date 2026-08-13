import { useSyncExternalStore } from 'react'

function subscribe(onChange: () => void): () => void {
  document.addEventListener('visibilitychange', onChange)
  return () => {
    document.removeEventListener('visibilitychange', onChange)
  }
}

function getSnapshot(): boolean {
  return document.visibilityState === 'visible'
}

/** True while the tab is visible. Used to stop polling in a hidden tab. */
export function usePageVisible(): boolean {
  return useSyncExternalStore(subscribe, getSnapshot, () => true)
}
