export default function withLogging(WrappedComponent, componentName = "Unknown") {
  return function LoggingWrapper(props) {
    console.log(`[AOP-LOG] Render: ${componentName}`, { props });
    return <WrappedComponent {...props} />;
  };
}


