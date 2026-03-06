from PIL import Image

mask_img = Image.open("cards_generation/256.png").convert("RGBA")
flag_img = Image.open("cards_generation/fr.png").convert("RGBA")

# récupérer la transparence
alpha = mask_img.getchannel("A")

# adapter le drapeau à la taille
flag_img = flag_img.resize(mask_img.size)

# créer l'image finale
result = Image.new("RGBA", mask_img.size)

# coller le drapeau avec le masque
result.paste(flag_img, (0,0), alpha)

result.show("resultat.png")