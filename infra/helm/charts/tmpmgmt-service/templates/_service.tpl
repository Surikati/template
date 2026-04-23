{{- define "tmpmgmt-service.service" -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ .Chart.Name }}
  labels: {{- include "tmpmgmt-service.labels" . | nindent 4 }}
spec:
  type: ClusterIP
  selector: {{- include "tmpmgmt-service.selectorLabels" . | nindent 4 }}
  ports:
    - name: http
      port: {{ .Values.service.port }}
      targetPort: http
      protocol: TCP
{{- end -}}
