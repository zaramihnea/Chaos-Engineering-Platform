import { useEffect } from "react";

export default function useFormMonitor(formState, rules) {
  useEffect(() => {
    Object.entries(rules).forEach(([field, rule]) => {
      const value = formState[field];

      const isValid = rule.check(value);

      if (!isValid) {
        console.warn(`MOP VIOLATION: ${rule.message}`);

        if (rule.onViolation) rule.onViolation(value);
      } else {
        if (rule.onValidation) rule.onValidation(value);
      }
    });
  }, [formState, rules]);
}

