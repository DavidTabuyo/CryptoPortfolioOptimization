import requests
import sys

def get_crypto_data(crypto_number: int):
    url = "https://min-api.cryptocompare.com/data/top/totalvolfull"
    parameters = {
        "limit": crypto_number,  # Obtener las N principales criptomonedas por capitalización de mercado
        "tsym": "USD"  # Obtener datos en dólares estadounidenses
    }

    try:
        response = requests.get(url, params=parameters)
        data = response.json()

        # Lista para almacenar los datos de cada criptomoneda
        crypto_data = []

        for crypto in data["Data"]:
            # Extraer los datos de cada criptomoneda
            crypto_name = crypto["CoinInfo"]["Name"]

            # Comprobación para verificar si los campos "RAW" y "USD" existen
            if "RAW" in crypto and "USD" in crypto["RAW"]:
                crypto_price = crypto["RAW"]["USD"].get("PRICE")
                crypto_change_24h = crypto["RAW"]["USD"].get("CHANGEPCT24HOUR")
                crypto_market_cap = crypto["RAW"]["USD"].get("MKTCAP")
                crypto_volume_24h = crypto["RAW"]["USD"].get("TOTALVOLUME24HTO")
            else:
                print(f"No se encontraron datos válidos para la criptomoneda '{crypto_name}'. Se omite.")
                continue  # Pasar al siguiente identificador sin incrementar su valor

            # Almacenar los datos en un diccionario
            crypto_info = {
                "Nombre": crypto_name,
                "Precio": crypto_price,
                "Cambio_porcentual_24h": crypto_change_24h,
                "Capitalizacion_mercado": crypto_market_cap,
                "Volumen_operaciones_24h": crypto_volume_24h
            }

            # Agregar los datos de la criptomoneda a la lista
            crypto_data.append(crypto_info)

        return crypto_data

    except Exception as e:
        print("Error al obtener datos de la API:", e)
        return None

def guardar_datos_criptomonedas(datos):
    if datos is None:
        print("No se pudieron obtener los datos de la API.")
        return

    with open("crypto_data.txt", "w") as archivo:
        identificador = 1
        for criptomoneda in datos:
            nombre = criptomoneda["Nombre"]

            identificador_str = f"cripto_{identificador}"
            archivo.write(f"{identificador_str}, {criptomoneda}\n")
            print(f"Datos de la criptomoneda '{nombre}' guardados en el identificador '{identificador_str}'")
            identificador += 1

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print('ERROR: Argumento no especificado')
        sys.exit(1)
    
    crypto_data = get_crypto_data(int(sys.argv[1]))
    guardar_datos_criptomonedas(crypto_data)
