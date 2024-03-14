import requests
import sys
import math

def is_stablecoin(price):
    return math.isclose(price, 1, rel_tol=0.01)

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

            # Excluir las stablecoins cuyo precio esté muy cerca de 1
            if is_stablecoin(crypto_price):
                print(f"La criptomoneda '{crypto_name}' es una stablecoin. Se omite.")
                continue

            # Realizar solicitudes adicionales para obtener los datos históricos
            historical_data = get_historical_data(crypto_name)

            # Actualizar los valores máximos y mínimos históricos si son mayores o menores que el valor actual
            if historical_data.get("Precio_max") is not None and historical_data["Precio_max"] < crypto_price:
                historical_data["Precio_max"] = crypto_price
            if historical_data.get("Precio_min") is not None and historical_data["Precio_min"] > crypto_price:
                historical_data["Precio_min"] = crypto_price
            if historical_data.get("Volumen_24h_max") is not None and historical_data["Volumen_24h_max"] < crypto_volume_24h:
                historical_data["Volumen_24h_max"] = crypto_volume_24h
            if historical_data.get("Volumen_24h_min") is not None and historical_data["Volumen_24h_min"] > crypto_volume_24h:
                historical_data["Volumen_24h_min"] = crypto_volume_24h

            # Almacenar los datos en un diccionario
            crypto_info = {
                "Nombre": crypto_name,
                "Precio": crypto_price,
                "Cambio_porcentual_24h": crypto_change_24h,
                "Capitalizacion_mercado": crypto_market_cap,
                "Volumen_operaciones_24h": crypto_volume_24h,
                "Precio_max_historico": historical_data.get("Precio_max"),
                "Precio_min_historico": historical_data.get("Precio_min"),
                "Volumen_24h_max_historico": historical_data.get("Volumen_24h_max"),
                "Volumen_24h_min_historico": historical_data.get("Volumen_24h_min")
            }

            # Agregar los datos de la criptomoneda a la lista
            crypto_data.append(crypto_info)

        return crypto_data

    except Exception as e:
        print("Error al obtener datos de la API:", e)
        return None

def get_historical_data(crypto_name: str):
    # Realizar solicitudes adicionales para obtener los datos históricos
    historical_url = f"https://min-api.cryptocompare.com/data/v2/histoday"
    historical_parameters = {
        "fsym": crypto_name,
        "tsym": "USD",
        "limit": 365  # Obtener datos históricos para el último año
    }

    try:
        response = requests.get(historical_url, params=historical_parameters)
        data = response.json()
        # Extraer los datos históricos
        historical_prices = [entry["close"] for entry in data["Data"]["Data"]]
        historical_volumes = [entry["volumeto"] for entry in data["Data"]["Data"]]

        # Calcular los valores máximos y mínimos históricos
        max_price = max(historical_prices)
        min_price = min(historical_prices)

        max_volume_24h = max(historical_volumes)
        min_volume_24h = min(historical_volumes)

        return {
            "Precio_max": max_price,
            "Precio_min": min_price,
            "Volumen_24h_max": max_volume_24h,
            "Volumen_24h_min": min_volume_24h
        }

    except Exception as e:
        print(f"Error al obtener datos históricos para '{crypto_name}':", e)
        return {
            "Precio_max": None,
            "Precio_min": None,
            "Volumen_24h_max": None,
            "Volumen_24h_min": None
        }

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
