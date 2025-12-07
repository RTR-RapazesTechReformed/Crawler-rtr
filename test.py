import re
import json
import asyncio
import nest_asyncio
from playwright.async_api import async_playwright
from openpyxl import Workbook

nest_asyncio.apply()

URL = "https://www.ligapokemon.com.br/?view=cards/search&tipo=1&card=ed=SVP+searchprod=0"


# --------------------------------------------------
# Função: Buscar os cards usando Playwright
# --------------------------------------------------
async def fetch_cards_playwright(url: str):
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page()

        print("Acessando página...")
        await page.goto(url, timeout=60000)
        await page.wait_for_load_state("networkidle")

        print("Página carregada, extraindo dados...")
        content = await page.content()
        await browser.close()

        match = re.search(r"var\s+cardsjson\s*=\s*(\[.*?\]);", content, re.DOTALL)
        if not match:
            raise ValueError("cardsjson não encontrado.")

        return json.loads(match.group(1))


# --------------------------------------------------
# Limpar nome EN removendo "(#006/∞)" mas mantendo "[Staff]"
# --------------------------------------------------
def clean_english_name(name: str):
    if not name:
        return name
    # remove id (#006/∞)
    name = re.sub(r"\s*\(#.*?\)", "", name)
    return name.strip()


# --------------------------------------------------
# Nome final: PT → EN (limpo)
# --------------------------------------------------
def get_card_name(card: dict):
    nome_pt = card.get("nPT")
    nome_en = card.get("nEN")

    # limpar nome EN antes do fallback
    if nome_en:
        nome_en = clean_english_name(nome_en)

    if not nome_pt or len(nome_pt.strip()) <= 1:
        return nome_en
    return nome_pt


# --------------------------------------------------
# Função principal
# --------------------------------------------------
async def main():
    cards = await fetch_cards_playwright(URL)
    print("Total de cards encontrados:", len(cards))

    wb = Workbook()
    ws = wb.active
    ws.title = "Cards"

    ws.append(["Nome da Carta", "Preço (menor)", "Preço (maior)", "Preço Médio"])

    for card in cards:

        nome = get_card_name(card)

        pmin = card.get("precoMenor")
        pmax = card.get("precoMaior")

        # converter para float se possível
        try:
            vmin = float(pmin)
            vmax = float(pmax)
            avg = (vmin + vmax) / 2
        except:
            avg = None

        ws.append([nome, pmin, pmax, avg])

    wb.save("cards.xlsx")
    print("Planilha 'cards.xlsx' criada com sucesso!")
    return cards


cards = asyncio.run(main())
cards[:3]
