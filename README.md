# Esp32Cam_4wd

Este é um mini carrinho/robô controlado por WiFi usando um aplicativo android. 
É usado um Esp32-Cam que atravéz do protocolo UDP realiza sua comunicação com o app, via WiFi.
O carrinho é um ponto de acesso WiFi. Então você conecta seu celular a ele, mas é possivel fazer o inverso também. 

A camera é a padrao que ja vem com o kit do Esp32-Cam, sendo ela a 2Mpixel Ov2640. Também existem versoes melhores de 5Mpx.
A taxa de dados roda na faixa dos 200 a 700Kbps, quando está em 25fps e com o frame de 800x600.
O MCU faz o controle da camera, captura a imagem no formato JPEG e envia pro android via UDP. 
O android usa o encoder padrão pra decodificar essa imagem, transforma em bitmap e depois mostra na tela.
A quantidade de fotos por segundo varia de acordo com o sinal do WiFi. O valor máximo é 25fps. Mas quando o sinal está frado, na ordem de -80dbm ou menos, o valor desce pra 5 a 15fps. Uma antena externa pode ser usada através do conector IPEX do Esp-32, aumentando a distância entra os dois dispositivos.

O circuito eletrônico foi desenhado no KiCad EDA - Schematic Capture & PCB Design Software.
A placa de circuito fisica foi desenhada usando uma CNC a laser, a TOTEM S 25 Laser Engraver, comprada no Aliexpress.

O app foi desenvolvido em Java, usando o Android Studio.

Foi testado no Galaxy A70 Android 11 e Motorola Moto E 7 Power - Android 10
O software do MCU foi desenvolvido no Arduino IDE.

![4wd_preview](https://user-images.githubusercontent.com/16022034/230798034-165acbf6-13e2-4ac6-9023-fda98a3277aa.jpg)
![app_preview](https://user-images.githubusercontent.com/16022034/230798040-5a017fa0-9976-49f5-8fd9-339535067b3f.jpg)
