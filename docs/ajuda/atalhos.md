# Atalhos de teclado

Esta lista reproduz os aceleradores do código e dos menus do NG. Alguns diferem dos padrões de outros programas. Quando o foco estiver num campo de texto, as teclas podem editar o texto; volte ao desenho ou use o menu para executar um comando do diagrama.

No macOS, onde esta página diz **Ctrl**, use **Command (⌘)**: os menus já mostram ⌘, e segurar Command ao clicar faz o que Ctrl faz nos outros sistemas. A exceção é **Ctrl+Tab**, que continua com Ctrl, porque Command+Tab troca de programa no macOS.

## Arquivo e ajuda

- **Ctrl+Shift+C**: novo Conceitual.
- **Ctrl+Shift+L**: novo Lógico.
- **Ctrl+Shift+F**: novo Fluxo.
- **Ctrl+Shift+A**: novo Atividade.
- **Ctrl+Shift+E**: novo EAP.
- **Ctrl+Shift+I**: novo Livre.
- **Ctrl+A**: Abrir.
- **Ctrl+B**: Salvar.
- **Ctrl+1**: Salvar Como….
- **Ctrl+2**: Salvar Todos.
- **Ctrl+F**: Fechar o diagrama.
- **Ctrl+P**: Imprimir….
- **Ctrl+E**: Exportar….
- **Ctrl+Q**: Sair.
- **F1**: Ajuda no navegador.

## Editar

- **Ctrl+Z**: Desfazer.
- **Ctrl+R**: Refazer.
- **Ctrl+X**: Recortar.
- **Ctrl+C**: Copiar.
- **Ctrl+V**: Colar.
- **Ctrl+I**: Copiar como imagem.
- **Ctrl+M**: Copiar formatação.
- **Ctrl+F**: Colar formatação.
- **Delete**: Apagar seleção.
- **Ctrl+T**: Selecionar Tudo.
- **Ctrl+N**: Selecionar todos deste tipo.
- **Ctrl+O**: Selecionar próximo.
- **Ctrl+E**: Selecionar anterior.
- **Ctrl+B**: Trazer para frente.
- **Ctrl+D**: Enviar para trás.
- **Ctrl+W**: Destacar seleção e relacionados.

Há colisões reais entre aceleradores: por exemplo, Ctrl+B aparece em Salvar e Trazer para frente, e Ctrl+F em Fechar e Colar formatação. Use o menu explícito quando o resultado depender do foco ou quando houver ambiguidade. Os valores configurados nos menus não significam que dois comandos com a mesma tecla possam executar simultaneamente.

## No desenho e nos diálogos

- **Setas**: mover a seleção; **Ctrl+seta** faz microajuste e **Shift+seta** redimensiona quando o objeto permite.
- **Esc**: voltar à seleção no desenho; nos editores que registram a tecla, fechar o diálogo.
- **Enter** no desenho: acessar a edição da propriedade Nome do selecionado.
- **Ctrl+Tab** / **Ctrl+Shift+Tab** com o desenho em foco: selecionar o próximo/anterior objeto.
- **Ctrl+Enter**: encerrar editores que registram essa combinação; confira o botão de conclusão de cada janela.

Fonte dos aceleradores: `src/principal/Propriedades_pt_BR.properties`, `controlador.Controler`, `controlador.Editor`, `controlador.Diagrama` e `principal.FramePrincipal`. Os comandos específicos do menu Diagrama não recebem automaticamente os atalhos comentados nas propriedades; por isso não estão listados aqui.

Volte a [Primeiros passos](primeiros-passos.md) para localizar os comandos na janela.
