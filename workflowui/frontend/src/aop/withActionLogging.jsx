export default function withActionLogging(actionName, fn) {
  return function (...args) {
    console.log(`[AOP-ACTION] Action triggered: ${actionName}`, { args });
    return fn(...args);
  };
}
