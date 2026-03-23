import json
import sys
import os

DASHBOARDS_DIR = os.path.join(os.path.dirname(__file__), "grafana", "dashboards")
PROMETHEUS_UID = "dffg89o4aqzggb"

def patch_jvm_dashboard():
    """JVM Micrometer 대시보드: application, instance, DS_PROMETHEUS 변수 초기화"""
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
                "text": "ujax-app:8080",
                "value": "ujax-app:8080"
            }
        elif var['name'] == 'DS_PROMETHEUS':
            var['current'] = {
                "selected": True,
                "text": "Prometheus",
                "value": PROMETHEUS_UID
            }

    with open(file_path, 'w') as f:
        json.dump(data, f, indent=2)
    print("  ✅ JVM Micrometer 대시보드 패치 완료")


def patch_node_exporter_dashboard():
    """Node Exporter 대시보드: datasource 변수를 우리 Prometheus UID로 고정"""
    file_path = os.path.join(DASHBOARDS_DIR, "node-exporter.json")
    with open(file_path, 'r') as f:
        data = json.load(f)

    # 1. 템플릿 변수의 datasource 기본값을 우리 Prometheus로 설정
    for var in data.get('templating', {}).get('list', []):
        if var['name'] == 'datasource':
            var['current'] = {
                "selected": True,
                "text": "Prometheus",
                "value": PROMETHEUS_UID
            }
        elif var['name'] == 'job':
            var['current'] = {
                "selected": True,
                "text": "node-exporter",
                "value": "node-exporter"
            }

    # 2. 모든 패널의 datasource 참조를 우리 Prometheus UID로 통일
    def fix_datasource(obj):
        if isinstance(obj, dict):
            if 'datasource' in obj:
                ds = obj['datasource']
                if isinstance(ds, dict) and ds.get('type') == 'prometheus':
                    ds['uid'] = PROMETHEUS_UID
                elif isinstance(ds, str) and ds in ('${datasource}', '${DS_PROMETHEUS}', 'default'):
                    obj['datasource'] = {"type": "prometheus", "uid": PROMETHEUS_UID}
            for v in obj.values():
                fix_datasource(v)
        elif isinstance(obj, list):
            for item in obj:
                fix_datasource(item)

    fix_datasource(data)

    with open(file_path, 'w') as f:
        json.dump(data, f, indent=2)
    print("  ✅ Node Exporter 대시보드 패치 완료")


if __name__ == "__main__":
    print("▶ Grafana 대시보드 자동 패치 시작...")
    patch_jvm_dashboard()
    patch_node_exporter_dashboard()
    print("▶ 모든 대시보드 패치 완료!")
