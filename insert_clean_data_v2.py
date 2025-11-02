import psycopg2
import json

print("📥 Insertando datos con mapeo correcto de columnas\n")

# Cargar datos limpios
with open('clean_data_to_insert.json', 'r', encoding='utf-8') as f:
    clean_data = json.load(f)

# Conectar
conn = psycopg2.connect(
    host="179.31.2.185",
    port=5432,
    database="hcen-periferico",
    user="webadmin",
    password="RXGyhe18615"
)
conn.autocommit = False
cursor = conn.cursor()

# Mapeo de columnas antiguas -> nuevas
COLUMN_MAPPING = {
    'usuario_salud': {
        'cedula': 'ci',  # cedula -> CI
        # Ignorar campos que ya no existen en el modelo nuevo
        'ignore': ['tipo_documento', 'primer_nombre', 'segundo_nombre', 
                   'primer_apellido', 'segundo_apellido', 'fecha_nac', 
                   'active', 'sincronizado_central', 'created_at', 'updated_at',
                   'tenant_id', 'fechanac']
    },
    'profesional_salud': {
        # Ignorar tenant_id, password, username
        'ignore': ['tenant_id', 'password', 'username']
    }
}

total_inserted = 0
table_order = ['clinica', 'administrador_clinica', 'configuracion_clinica', 
               'usuario_salud', 'profesional_salud']

for table_name in table_order:
    if table_name not in clean_data or not clean_data[table_name]:
        print(f"⏭️  Saltando {table_name}")
        continue
    
    print(f"\n📊 Insertando en {table_name.upper()}...")
    
    mapping = COLUMN_MAPPING.get(table_name, {})
    ignore_cols = mapping.get('ignore', [])
    col_rename = {k: v for k, v in mapping.items() if k != 'ignore'}
    
    inserted = 0
    for record in clean_data[table_name]:
        try:
            # Aplicar mapeo de columnas
            mapped_record = {}
            for col, val in record.items():
                if col in ignore_cols:
                    continue
                new_col = col_rename.get(col, col)
                if val is not None and val != 'None':
                    mapped_record[new_col] = val
            
            if not mapped_record:
                continue
            
            columns = list(mapped_record.keys())
            values = list(mapped_record.values())
            
            placeholders = ','.join(['%s'] * len(columns))
            columns_str = ','.join([f'"{col}"' for col in columns])
            
            query = f'INSERT INTO "{table_name}" ({columns_str}) VALUES ({placeholders})'
            cursor.execute(query, values)
            inserted += 1
            
        except Exception as e:
            print(f"   ⚠️  Error: {e}")
            print(f"      Record: {record}")
            conn.rollback()
            continue
    
    conn.commit()
    print(f"   ✓ {inserted} registros insertados")
    total_inserted += inserted

print("\n" + "=" * 80)
print(f"✅ Total: {total_inserted} registros")
print("=" * 80)

cursor.close()
conn.close()
