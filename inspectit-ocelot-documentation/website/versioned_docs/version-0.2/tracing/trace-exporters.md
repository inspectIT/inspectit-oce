---
id: version-0.2-trace-exporters
title: Trace Exporters
original_id: trace-exporters
---

Metrics exporters are responsible for passing the recorded tracing data to a corresponding storage.

inspectIT Ocelot currently supports the following OpenCensus trace exporters:

* [ZipKin](#zipkin-exporter) [[Homepage](https://zipkin.io/)]
* [Jaeger](#jaeger-exporter) [[Homepage](https://www.jaegertracing.io/)]
* [OpenCensus Agent](#opencensus-agent-trace-exporter) [[Homepage](https://opencensus.io/exporters/supported-exporters/java/ocagent/)]

## ZipKin Exporter

The ZipKin exporter exports Traces in ZipKin v2 format to a ZipKin server or other compatible servers.
It can be enabled and disabled via the `inspectit.exporters.tracing.zipkin.enabled` property. By default, the ZipKin exporter is enabled. It however does not have an URL configured. The exporter will start up as soon as you define the `inspectit.exporters.tracing.zipkin.url` property.

For example, when adding the following property to your `-javaagent` options, traces will be sent to a zipkin server running on your localhost with the default port:

```
-Dinspectit.exporters.tracing.zipkin.url=http://127.0.0.1:9411/api/v2/spans
```

When sending spans, ZipKin expects you to give a name of the service where the spans have been recorded. This name can be set using the `inspectit.exporters.tracing.zipkin.service-name` property. This property defaults to `inspectit.service-name`.


## Jaeger Exporter

The Jaeger exports works exactly the same way as the [ZipKin Exporter](#zipkin-exporter).
The corresponding properties are the following:

* `inspectit.exporters.tracing.jaeger.enabled`: enables / disables the Jaeger exporter
* `inspectit.exporters.tracing.jaeger.url`: defines the URL where the spans will be pushed
* `inspectit.exporters.tracing.jaeger.service-name`: defines the service name under which the spans will be published

By default, the Jaeger exporter is enabled but has no URL configured.
The service name defaults to `inspectit.service-name`.

To make inspectIT Ocelot push the spans to a Jaeger server running on the same machine as the agent, the following JVM property can be used:

```
-Dinspectit.exporters.tracing.jaeger.url=http://127.0.0.1:14268/api/traces
```

## OpenCensus Agent Trace Exporter

Spans can be additionally exported to the [OpenCensus Agent](https://opencensus.io/service/components/agent/).
When enabled, all Spans are send via gRCP to the OpenCensus Agent. By default, the exporter is enabled, but the agent address is set to `null`.

|Property |Default| Description
|---|---|---|
|`inspectit.exporters.tracing.open-census-agent.enabled`|`true`|If true, the agent will try to start the OpenCensus Agent Trace exporter.
|`inspectit.exporters.tracing.open-census-agent.address`|`null`|Address of the open-census agent (e.g. localhost:1234).
|`inspectit.exporters.tracing.open-census-agent.use-insecure`|`false`|If true, SSL is disabled.
|`inspectit.exporters.tracing.open-census-agent.service-name`|refers to `inspectit.service-name`|The service-name which will be used to publish the spans.
|`inspectit.exporters.tracing.open-census-agent.reconnection-period`|`5`|The time at which the exporter tries to reconnect to the OpenCensus agent.

> Don't forget to check [the official OpenCensus Agent exporter documentation](https://opencensus.io/exporters/supported-exporters/java/ocagent/).