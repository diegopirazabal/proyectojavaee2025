import psycopg2
import json
from datetime import datetime

print("📥 Insertando datos limpios en la base de datos\n")

# Cargar datos limpios
with open('clean_data_to_insert.json', 'r', encoding='utf-8') as f:
    clean_data = json.load(f)

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

# Orden de inserción (respetando foreign keys)
table_order = ['clinica', 'administrador_clinica', 'configuracion_clinica', 
               'usuario_salud', 'profesional_salud']

total_inserted = 0

for table_name in table_order:
    if table_name not in clean_data or not clean_data[table_name]:
        print(f"⏭️  Saltando {table_name} (sin datos)")
        continue
    
    print(f"\n📊 Insertando en {table_name.upper()}...")
    
    records = clean_data[table_name]
    inserted = 0
    
    for record in records:
        try:
            # Construir query dinámicamente
            columns = [col for col, val in record.items() if val is not None and val != 'None']
            values = [record[col] for col in columns]
            
            placeholders = ','.join(['%s'] * len(columns))
            columns_str = ','.join([f'"{col}"' for col in columns])
            
            query = f'INSERT INTO "{table_name}" ({columns_str}) VALUES ({placeholders})'
            cursor.execute(query, values)
            inserted += 1
            
        except Exception as e:
            print(f"   ⚠️  Error insertando registro: {e}")
            print(f"      Datos: {record}")
            conn.rollback()
            continue
    
    conn.commit()
    print(f"   ✓ {inserted} registros insertados")
    total_inserted += inserted

print("\n" + "=" * 80)
print(f"✅ RESTAURACIÓN COMPLETADA")
print(f"   Total de registros insertados: {total_inserted}")
print("=" * 80)

cursor.close()
conn.close()
