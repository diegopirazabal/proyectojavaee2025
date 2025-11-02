import json
import glob

# Encontrar el archivo de backup más reciente
backup_files = glob.glob('backup_hcen_periferico_*.json')
if not backup_files:
    print("No se encontró archivo de backup")
    exit(1)

backup_file = sorted(backup_files)[-1]
print(f"Analizando: {backup_file}\n")

with open(backup_file, 'r', encoding='utf-8') as f:
    backup_data = json.load(f)

# Analizar cada tabla
print("=" * 80)
print("ANÁLISIS DE DATOS")
print("=" * 80)

for table_name, table_data in backup_data.items():
    if table_data['row_count'] == 0:
        continue
    
    print(f"\n📊 Tabla: {table_name.upper()}")
    print(f"   Registros: {table_data['row_count']}")
    
    # Verificar columnas con valores NULL
    null_columns = {}
    for row in table_data['data']:
        for col_name, value in row.items():
            if value is None or value == 'None':
                if col_name not in null_columns:
                    null_columns[col_name] = 0
                null_columns[col_name] += 1
    
    if null_columns:
        print("   ⚠️  Columnas con NULL:")
        for col, count in null_columns.items():
            col_info = next((c for c in table_data['columns'] if c['name'] == col), None)
            nullable = col_info['nullable'] if col_info else 'YES'
            print(f"      - {col}: {count} nulls (nullable={nullable})")
    
    # Mostrar primeros registros
    if table_data['row_count'] > 0 and table_data['row_count'] <= 3:
        print("   📋 Datos:")
        for i, row in enumerate(table_data['data'], 1):
            print(f"      Registro {i}:")
            for k, v in row.items():
                if v and v != 'None':
                    print(f"         {k}: {v}")

print("\n" + "=" * 80)
print("PLAN DE LIMPIEZA")
print("=" * 80)

# Plan para USUARIO_SALUD
if 'usuario_salud' in backup_data and backup_data['usuario_salud']['row_count'] > 0:
    print("\n🔧 USUARIO_SALUD:")
    print("   - Agregar valores default para CI (usar incremental desde 10000000)")
    print("   - Agregar valores default para 'apellidos' (usar 'Pendiente')")
    print("   - Agregar valores default para 'nombre' si es NULL")

# Plan para PROFESIONAL_SALUD
if 'profesional_salud' in backup_data and backup_data['profesional_salud']['row_count'] > 0:
    print("\n🔧 PROFESIONAL_SALUD:")
    print("   - Verificar que CI sea único y NOT NULL")

# Plan para ADMINISTRADOR_CLINICA
if 'administrador_clinica' in backup_data and backup_data['administrador_clinica']['row_count'] > 0:
    print("\n🔧 ADMINISTRADOR_CLINICA:")
    print("   - Verificar tenant_id y asignación a clínicas")

print("\n" + "=" * 80)
