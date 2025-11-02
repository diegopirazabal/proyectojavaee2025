import psycopg2
import json
from datetime import datetime

# Conexión a la base de datos
conn = psycopg2.connect(
    host="179.31.2.185",
    port=5432,
    database="hcen-periferico",
    user="webadmin",
    password="RXGyhe18615"
)

cursor = conn.cursor()

# Obtener todas las tablas
cursor.execute("""
    SELECT table_name 
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_type = 'BASE TABLE'
    ORDER BY table_name
""")

tables = [row[0] for row in cursor.fetchall()]

print(f"Tablas encontradas: {len(tables)}")
for table in tables:
    print(f"  - {table}")

backup_data = {}

# Para cada tabla, obtener estructura y datos
for table in tables:
    print(f"\nExportando tabla: {table}")
    
    # Obtener columnas
    cursor.execute(f"""
        SELECT column_name, data_type, is_nullable, column_default
        FROM information_schema.columns 
        WHERE table_name = '{table}'
        ORDER BY ordinal_position
    """)
    
    columns_info = cursor.fetchall()
    columns = [col[0] for col in columns_info]
    
    # Obtener datos
    cursor.execute(f'SELECT * FROM "{table}"')
    rows = cursor.fetchall()
    
    # Convertir a formato serializable
    rows_serializable = []
    for row in rows:
        row_dict = {}
        for i, val in enumerate(row):
            if isinstance(val, datetime):
                row_dict[columns[i]] = val.isoformat()
            else:
                row_dict[columns[i]] = str(val) if val is not None else None
        rows_serializable.append(row_dict)
    
    backup_data[table] = {
        'columns': [{'name': col[0], 'type': col[1], 'nullable': col[2], 'default': col[3]} 
                    for col in columns_info],
        'row_count': len(rows),
        'data': rows_serializable
    }
    
    print(f"  ✓ {len(rows)} registros exportados")

# Guardar backup
timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
filename = f'backup_hcen_periferico_{timestamp}.json'

with open(filename, 'w', encoding='utf-8') as f:
    json.dump(backup_data, f, indent=2, ensure_ascii=False)

print(f"\n✓ Backup completo guardado en: {filename}")
print(f"✓ Total de tablas: {len(tables)}")
print(f"✓ Total de registros: {sum(t['row_count'] for t in backup_data.values())}")

cursor.close()
conn.close()
