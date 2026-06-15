Ministério da Educação **Universidade Tecnológica Federal do Paraná** Campus de Medianeira Curso de Bacharelado em Ciência da Computação

**Universidade Tecnológica Federal do Paraná Lista** Campus de Medianeira **Disciplina: CC57E - Sistemas Distribuídos** Curso de Bacharelado em Ciência da Computação Acadêmico(a): ___________________________________________________________ Professor(a) Fernando Schütz, MSc

## **Exercícios* Java RMI**

Escreva um servidor Java RMI que disponibilize um método específico para realizar cada uma das operações a seguir sobre uma _string_ S, fornecida como parâmetro na chamada dos métodos:

- P : verifica se S é um palíndromo; retorna _true_ ou _false_ .
- I : gera uma nova _string_ na qual os caracteres de S aparecem na ordem inversa; retorna a _string_ gerada.
- + : gera uma nova _string_ na qual as letras minúsculas presentes em S aparecem em maiúsculas; retorna a _string_ gerada.
- : gera uma nova _string_ na qual as letras maiúsculas presentes em S aparecem em minúsculas; retorna a _string_ gerada.
- V : descobre a quantidade de vogais presentes em S; retorna o número inteiro obtido.
- C : descobre a quantidade de consoantes presentes em S; retorna o número inteiro obtido.
- A : gera uma nova _string_ na qual constam somente as vogais presentes em S; retorna a _string_ gerada.
- Z : gera uma nova _string_ na qual constam somente as consoantes presentes em S; retorna a _string_ gerada.
- W : descobre o local em S onde conste, pela primeira vez, um certo caractere fornecido como parâmetro; retorna um inteiro correspondente ao local, o qual deve ser –1 quando o caracter não consta em S.
- F : descobre o local em S onde se inicie, pela primeira vez, uma certa _substring_ fornecida como parâmetro; retorna um inteiro correspondente ao local, o qual deve ser –1 quando a _substring_ não consta em S.

As funções acima devem ser chamadas a partir de um cliente Java RMI, a qual deve capturar todos os argumentos necessários para a execução de cada função, assim como o próprio código que identifica a função, através do vetor de argumentos (args). Por exemplo, supondo que a classe cliente chame-se m  e que esta seja iniciada usando “comando de linha” (como em um prompt DOS), o seguinte comando faz uso da operação F :

- $ java m F “sistemas termicos” “te”

Resposta: 4

- $
