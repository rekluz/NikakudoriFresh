import os

res_dir = r"c:\Android_Projects\Nikakudori MahjongV5\app\src\main\res"

unused_keys = [
    "game_paused",
    "loading_board",
    "btn_play_again",
    "board_size_format",
    "about_hello_world",
    "current_score_label",
    "time_label",
    "placeholder_rankings",
    "about_developer",
    "copyright_notice",
    "github_url"
]

translations = {
    "es": {
        "setting_music": "Música",
        "setting_board": "Tablero",
        "boards_2d": "Tableros 2D",
        "boards_3d": "Tableros 3D",
        "board_pyramid": "Pirámide",
        "board_fortress": "Fortaleza",
        "board_turtle": "Tortuga",
        "board_bridge": "Puente",
        "board_dragon": "Dragón",
        "board_castle": "Castillo"
    },
    "fr": {
        "setting_music": "Musique",
        "setting_board": "Plateau",
        "boards_2d": "Plateaux 2D",
        "boards_3d": "Plateaux 3D",
        "board_pyramid": "Pyramide",
        "board_fortress": "Forteresse",
        "board_turtle": "Tortue",
        "board_bridge": "Pont",
        "board_dragon": "Dragon",
        "board_castle": "Château"
    },
    "it": {
        "setting_music": "Musica",
        "setting_board": "Tavolo",
        "boards_2d": "Tavoli 2D",
        "boards_3d": "Tavoli 3D",
        "board_pyramid": "Piramide",
        "board_fortress": "Fortezza",
        "board_turtle": "Tartaruga",
        "board_bridge": "Ponte",
        "board_dragon": "Drago",
        "board_castle": "Castello"
    },
    "tl": {
        "setting_music": "Musika",
        "setting_board": "Board",
        "boards_2d": "Mga 2D Board",
        "boards_3d": "Mga 3D Board",
        "board_pyramid": "Piramide",
        "board_fortress": "Kuta",
        "board_turtle": "Pagong",
        "board_bridge": "Tulay",
        "board_dragon": "Dragon",
        "board_castle": "Kastilyo"
    }
}

for root, dirs, files in os.walk(res_dir):
    if "strings.xml" in files and "values" in root:
        filepath = os.path.join(root, "strings.xml")
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        new_lines = []
        for line in lines:
            if "<string name=\"btn_cancel\">CANCEL</string>" in line:
                new_lines.append(line.replace("CANCEL", "Cancel"))
                continue
                
            skip = False
            for k in unused_keys:
                if f'<string name="{k}"' in line:
                    skip = True
                    break
            if not skip:
                new_lines.append(line)
        
        lang = os.path.basename(root).replace("values-", "")
        if lang in translations:
            items = translations[lang]
            append_str = ""
            for k, v in items.items():
                if f'name="{k}"' not in "".join(new_lines):
                    append_str += f'    <string name="{k}">{v}</string>\n'
            
            if append_str:
                for i in reversed(range(len(new_lines))):
                    if "</resources>" in new_lines[i]:
                        new_lines.insert(i, append_str)
                        break
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.writelines(new_lines)
            
print("Strings updated")
