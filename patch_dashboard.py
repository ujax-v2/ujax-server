import json
import os

DASHBOARDS_DIR = os.path.join(os.path.dirname(__file__), "grafana", "dashboards")

# ──────────────────────────────────────────────────────────────────
# Grafana Cloud Prometheus 데이터소스 UID 확인 방법:
#   Grafana Cloud UI → Connections → Data Sources → grafanacloud-ujax-prom 클릭
#   URL: .../connections/datasources/edit/XXXXXXX  ← 이 XXXXXXX 값
# ──────────────────────────────────────────────────────────────────
PROMETHEUS_UID = "grafanacloud-prom"


def fix_datasource_refs(obj, template_var_name):
    """모든 패널의 datasource uid를 템플릿 변수 참조로 교체 (하드코딩 UID 제거)"""
    if isinstance(obj, dict):
        if 'datasource' in obj:
            ds = obj['datasource']
            if isinstance(ds, dict) and ds.get('type') == 'prometheus':
                ds['uid'] = f"${{{template_var_name}}}"
            elif isinstance(ds, str) and ds not in ('', None):
                obj['datasource'] = {"type": "prometheus", "uid": f"${{{template_var_name}}}"}
        for v in obj.values():
            fix_datasource_refs(v, template_var_name)
    elif isinstance(obj, list):
        for item in obj:
            fix_datasource_refs(item, template_var_name)


def patch_jvm_dashboard():
    """JVM Micrometer 대시보드 패치:
    - instance: ujax-app:8080 → localhost:8080 (Alloy가 호스트에서 스크랩)
    - DS_PROMETHEUS: Grafana Cloud UID로 업데이트
    - 모든 패널 datasource: ${DS_PROMETHEUS} 템플릿 변수 참조로 통일
    """
    file_path = os.path.join(DASHBOARDS_DIR, "jvm-micrometer.json")
    with open(file_path, 'r') as f:
        data = json.load(f)

    for var in data.get('templating', {}).get('list', []):
        if var['name'] == 'application':
            var['current'] = {
                "selected": True,
                "text": "ujax-server",
                "value": "ujax-server"
            }
        elif var['name'] == 'instance':
            var['current'] = {
                "selected": True,
                "text": "localhost:8080",
                "value": "localhost:8080"
            }
        elif var['name'] == 'DS_PROMETHEUS':
            var['current'] = {
                "selected": True,
                "text": "grafanacloud-ujax-prom",
                "value": PROMETHEUS_UID
            }

    # 패널 datasource를 ${DS_PROMETHEUS} 참조로 통일
    fix_datasource_refs(data, "DS_PROMETHEUS")

    with open(file_path, 'w') as f:
        json.dump(data, f, indent=2)
    print("  ✅ JVM Micrometer 대시보드 패치 완료 (instance=localhost:8080)")


def patch_node_exporter_dashboard():
    """Node Exporter 대시보드 패치:
    - job: node-exporter → node-exporter-app (Alloy job_name 변경)
    - datasource: Grafana Cloud UID로 업데이트
    - 모든 패널 datasource: ${datasource} 템플릿 변수 참조로 통일
    """
    file_path = os.path.join(DASHBOARDS_DIR, "node-exporter.json")
    with open(file_path, 'r') as f:
        data = json.load(f)

    for var in data.get('templating', {}).get('list', []):
        if var['name'] == 'datasource':
            var['current'] = {
                "selected": True,
                "text": "grafanacloud-ujax-prom",
                "value": PROMETHEUS_UID
            }
        elif var['name'] == 'job':
            var['current'] = {
                "selected": True,
                "text": "node-exporter-app",
                "value": "node-exporter-app"
            }

    # 패널 datasource를 ${datasource} 참조로 통일
    fix_datasource_refs(data, "datasource")

    with open(file_path, 'w') as f:
        json.dump(data, f, indent=2)
    print("  ✅ Node Exporter 대시보드 패치 완료 (job=node-exporter-app)")


if __name__ == "__main__":
    print("▶ Grafana 대시보드 자동 패치 시작...")
    patch_jvm_dashboard()
    patch_node_exporter_dashboard()
    print("▶ 모든 대시보드 패치 완료! 이제 Grafana Cloud에 다시 임포트하세요.")
