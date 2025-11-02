import psycopg2
import json
import glob
from datetime import datetime

# Encontrar el archivo de backup más reciente
backup_files = glob.glob('backup_hcen_periferico_*.json')
backup_file = sorted(backup_files)[-1]
print(f"📁 Restaurando desde: {backup_file}\n")

with open(backup_file, 'r', encoding='utf-8') as f:
    backup_data = json.load(f)

# Conectar a la base de datos
conn = psycopg2.connect(
    host="179.31.2.185",
    port=5432,
    database="hcen-periferico",
    user="webadmin",
    password="RXGyhe18615"
)
conn.autocommit = False
cursor = conn.cursor()

print("🗑️  PASO 1: Limpiando base de datos actual...")
print("=" * 80)

# Eliminar todas las tablas en orden correcto (foreign keys primero)
tables_to_drop = [
    'politica_acceso', 'notificacion', 'documento_clinico', 'historia_clinica',
    'usuario_clinica', 'clinica_profesional', 'credencial_profesional', 
    'sincronizacion_pendiente', 'jwt_sessions', 'configuracion_clinica',
    'usuario_salud', 'profesional_salud', 'administrador_clinica', 
    'administrador_hcen', 'clinica'
]

for table in tables_to_drop:
    try:
        cursor.execute(f'DROP TABLE IF EXISTS "{table}" CASCADE')
        print(f"   ✓ Tabla {table} eliminada")
    except Exception as e:
        print(f"   ⚠️  Error al eliminar {table}: {e}")

conn.commit()

print("\n✅ Base de datos limpia")
print("\n🔧 PASO 2: Dejando que Hibernate cree el esquema...")
print("=" * 80)
print("⏸️  Ahora debes:")
print("   1. Hacer un redeploy en Railway")
print("   2. Esperar a que Hibernate cree todas las tablas con el esquema correcto")
print("   3. Ejecutar el script 'insert_clean_data.py' para insertar los datos limpios")

# Preparar datos limpios para inserción posterior
clean_data = {}

# Limpiar USUARIO_SALUD
if 'usuario_salud' in backup_data:
    usuarios = []
    base_ci = 10000000
    for i, row in enumerate(backup_data['usuario_salud']['data']):
        clean_row = row.copy()
        # Agregar CI si no existe
        if not clean_row.get('ci') or clean_row['ci'] == 'None':
            clean_row['ci'] = str(base_ci + i)
        # Agregar nombre si es NULL
        if not clean_row.get('nombre') or clean_row['nombre'] == 'None':
            clean_row['nombre'] = 'Usuario'
        # Agregar apellidos si es NULL
        if not clean_row.get('apellidos') or clean_row['apellidos'] == 'None':
            clean_row['apellidos'] = 'Pendiente'
        usuarios.append(clean_row)
    clean_data['usuario_salud'] = usuarios

# Limpiar PROFESIONAL_SALUD
if 'profesional_salud' in backup_data:
    profesionales = []
    for row in backup_data['profesional_salud']['data']:
        clean_row = row.copy()
        # username y password pueden ser NULL según el modelo
        profesionales.append(clean_row)
    clean_data['profesional_salud'] = profesionales

# Copiar otras tablas sin modificar
for table in ['administrador_clinica', 'clinica', 'configuracion_clinica', 'jwt_sessions']:
    if table in backup_data and backup_data[table]['row_count'] > 0:
        clean_data[table] = backup_data[table]['data']

# Guardar datos limpios
clean_file = 'clean_data_to_insert.json'
with open(clean_file, 'w', encoding='utf-8') as f:
    json.dump(clean_data, f, indent=2, ensure_ascii=False)

print(f"\n💾 Datos limpios guardados en: {clean_file}")
print(f"   Total de registros a restaurar: {sum(len(v) for v in clean_data.values())}")

cursor.close()
conn.close()
