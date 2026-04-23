{{- define "tmpmgmt-service.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .Chart.Name }}
  labels: {{- include "tmpmgmt-service.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicaCount | default 1 }}
  selector:
    matchLabels: {{- include "tmpmgmt-service.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels: {{- include "tmpmgmt-service.selectorLabels" . | nindent 8 }}
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "{{ .Values.service.port }}"
        prometheus.io/path: /actuator/prometheus
    spec:
      {{- with .Values.global.image.pullSecrets }}
      imagePullSecrets: {{ toYaml . | nindent 8 }}
      {{- end }}
      containers:
        - name: app
          image: {{ include "tmpmgmt-service.image" . }}
          imagePullPolicy: {{ .Values.global.image.pullPolicy | default "IfNotPresent" }}
          ports:
            - name: http
              containerPort: {{ .Values.service.port }}
          env:
            - name: KEYCLOAK_ISSUER_URI
              value: {{ .Values.global.keycloak.issuerUri | quote }}
            - name: RABBITMQ_HOST
              value: {{ .Values.global.rabbitmq.host | quote }}
            - name: RABBITMQ_PORT
              value: {{ .Values.global.rabbitmq.port | quote }}
            {{- with .Values.env }}
            {{- toYaml . | nindent 12 }}
            {{- end }}
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: http }
            initialDelaySeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: http }
            initialDelaySeconds: 30
            periodSeconds: 30
          resources: {{- toYaml (.Values.resources | default dict) | nindent 12 }}
{{- end -}}
