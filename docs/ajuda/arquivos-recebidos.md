# Abrir arquivos de outras pessoas

Professores abrem os trabalhos de turmas inteiras; alunos trocam diagramas entre si e recebem modelos do professor. Um diagrama é um arquivo que o programa interpreta, e um arquivo preparado pode tentar abusar disso. Esta página explica o que o NG faz por você e o que continua dependendo do seu cuidado.

## O que o NG faz

- **Lê o arquivo num processo separado.** Esse processo aceita só o que um diagrama do brModelo contém e recusa o resto, em vez de executá-lo. No Linux, ele não tem rede nem acesso às suas pastas.
- **Não trava a janela.** Um arquivo grande demais, danificado ou feito para travar o programa encerra só a leitura, que tem limite de memória e de tempo. Se a abertura passar de meio segundo, aparece **Abrindo diagrama**, com **Cancelar**.
- **Não usa a internet.** O NG não faz nenhuma chamada de rede, então um arquivo não consegue avisar ninguém de que foi aberto. Nos pacotes Flatpak, `.deb`, `.rpm`, `.tar.gz` e `.msi`, uma segunda barreira bloqueia a rede do programa.
- **Gera SQL sem comandos escondidos em nomes.** Um nome de tabela que poderia carregar um comando sai entre aspas.
- **Não corta o arquivo ao salvar.** Se a gravação falhar, o arquivo anterior fica intacto.

Se o arquivo for recusado, nada é aberto e o motivo fica no indicador de log; veja [Problemas comuns](problemas-comuns.md#o-arquivo-demora-a-abrir-ou-foi-recusado).

## Nada substitui o cuidado de quem abre

O NG protege só o que ele mesmo abre, e só contra os problemas que conhecemos. Estes hábitos valem mais do que qualquer proteção:

1. **Confira se é mesmo um diagrama.** Diagramas terminam em `.brM3`, `.brMj` ou `.xml`. `trabalho.brM3.exe`, `.jar`, `.bat`, `.cmd`, `.lnk`, `.scr`, `.sh` ou um `.zip` com um programa dentro não são diagramas: abri-los executa um programa, e nenhum editor protege disso. No Windows, ative **Extensões de nomes de arquivos** no Explorador de Arquivos para ver o nome inteiro.
2. **Abra pelo NG**, por **Arquivo → Abrir** ou pelo gerenciador de arquivos. Para arquivos de outras pessoas, prefira o Flatpak no Linux. No Windows, prefira o `.msi` se quiser que o instalador aplique automaticamente as regras do firewall que bloqueiam a rede do programa.
3. **Não abra no brModelo oficial um arquivo que o NG recusou.** O oficial não tem nenhuma destas proteções. Peça a quem enviou que salve o diagrama de novo.
4. **Receba pelos canais da disciplina**, como o ambiente virtual ou o e-mail da instituição. Desconfie de arquivo inesperado, mesmo de um nome conhecido: contas também são invadidas.
5. **Leia o SQL gerado de um diagrama recebido antes de executá-lo.** Tipos e complementos de campo são SQL escrito no próprio diagrama. Execute o script num banco de testes, nunca num banco com dados que importam.
6. **Salve o que está aberto** antes de abrir arquivos de outras pessoas e mantenha o NG e o sistema atualizados.
7. **No Linux, prefira a sessão Wayland.** Na sessão X11, programas com janela podem enviar teclas a outras janelas; veja [Problemas comuns](problemas-comuns.md#apareceu-um-aviso-de-sessão-x11).

## Para quem entrega

Entregue o arquivo salvo pelo programa, com a extensão original. Para o professor que usa o brModelo oficial, salve em `.brM3`: o NG mantém o formato, e o oficial não lê `.brMj`. Não renomeie a extensão nem compacte o diagrama junto com programas.
