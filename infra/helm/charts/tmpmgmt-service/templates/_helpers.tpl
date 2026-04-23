{{/*
Common labels.
*/}}
{{- define "tmpmgmt-service.labels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | default "0.1.0" | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: template-management
{{- end -}}

{{- define "tmpmgmt-service.selectorLabels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "tmpmgmt-service.image" -}}
{{- $global := .Values.global | default dict -}}
{{- $img := .Values.image | default dict -}}
{{- $registry := $img.registry | default $global.image.registry -}}
{{- $repo := printf "%s/%s" ($global.image.repositoryPrefix | default "template-management") .Chart.Name -}}
{{- $tag := $img.tag | default $global.image.tag | default .Chart.AppVersion -}}
{{- printf "%s/%s:%s" $registry $repo $tag -}}
{{- end -}}
