/* AEP6S — tela de registro de doações.
   Fala com a própria API por caminhos relativos (/api/...), então não existe CORS. */

const API = '/api';

const estado = {
  usuarios: [],
  doacoes: [],
  resumo: null,
};

class ApiError extends Error {
  constructor(mensagem, status, corpo) {
    super(mensagem);
    this.name = 'ApiError';
    this.status = status;
    this.corpo = corpo;
  }
}

/* ---------------- utilidades ---------------- */

const id = (nome) => document.getElementById(nome);

function criar(tag, classe, texto) {
  const no = document.createElement(tag);
  if (classe) no.className = classe;
  if (texto !== undefined) no.textContent = texto;
  return no;
}

function numero(valor) {
  return new Intl.NumberFormat('pt-BR').format(valor ?? 0);
}

function formatarData(iso) {
  if (!iso) return '—';
  const partes = String(iso).split('-');
  if (partes.length !== 3) return iso;
  return `${partes[2]}/${partes[1]}/${partes[0]}`;
}

function hojeIso() {
  const agora = new Date();
  const mes = String(agora.getMonth() + 1).padStart(2, '0');
  const dia = String(agora.getDate()).padStart(2, '0');
  return `${agora.getFullYear()}-${mes}-${dia}`;
}

/* ---------------- status, espera e avisos ---------------- */

function definirStatus(novoEstado) {
  const bloco = id('api-status');
  const texto = id('api-status-texto');
  bloco.dataset.state = novoEstado;
  if (novoEstado === 'online') texto.textContent = 'API conectada';
  else if (novoEstado === 'offline') texto.textContent = 'API indisponível';
  else texto.textContent = 'Conectando à API…';
}

let requisicoesPendentes = 0;
let timerAvisoLento = null;

function marcarEspera(delta) {
  requisicoesPendentes = Math.max(0, requisicoesPendentes + delta);
  if (requisicoesPendentes > 0) {
    if (!timerAvisoLento) {
      timerAvisoLento = window.setTimeout(() => {
        id('aviso-lento').hidden = false;
      }, 4000);
    }
    return;
  }
  window.clearTimeout(timerAvisoLento);
  timerAvisoLento = null;
  id('aviso-lento').hidden = true;
}

function toast(mensagem, tipo = 'ok') {
  const caixa = criar('div', tipo === 'erro' ? 'toast toast--erro' : 'toast', mensagem);
  id('toasts').append(caixa);
  window.setTimeout(() => {
    caixa.classList.add('toast--saindo');
    window.setTimeout(() => caixa.remove(), 220);
  }, 4200);
}

/* ---------------- cliente da API ---------------- */

async function api(caminho, opcoes = {}) {
  const config = { ...opcoes, headers: { ...(opcoes.headers || {}) } };
  if (config.body !== undefined) {
    config.headers['Content-Type'] = 'application/json';
    config.body = JSON.stringify(config.body);
  }

  marcarEspera(1);
  let resposta;
  try {
    resposta = await fetch(API + caminho, config);
  } catch (erro) {
    definirStatus('offline');
    throw new ApiError('Não foi possível falar com a API. Verifique a conexão e tente de novo.', 0, null);
  } finally {
    marcarEspera(-1);
  }

  definirStatus('online');

  if (resposta.status === 204) return null;

  const tipo = resposta.headers.get('content-type') || '';
  const corpo = tipo.includes('application/json')
    ? await resposta.json().catch(() => null)
    : null;

  if (!resposta.ok) {
    throw new ApiError(mensagemDoErro(resposta.status, corpo), resposta.status, corpo);
  }
  return corpo;
}

function mensagemDoErro(status, corpo) {
  if (corpo && typeof corpo === 'object') {
    if (typeof corpo.erro === 'string' && corpo.erro) return corpo.erro;
    const mensagens = Object.values(corpo).filter((v) => typeof v === 'string');
    if (mensagens.length === 1) return mensagens[0];
    if (mensagens.length > 1) return 'Confira os campos destacados.';
  }
  if (status === 404) return 'Registro não encontrado. Ele pode ter sido removido em outra aba.';
  return `A API respondeu com erro ${status}.`;
}

/* ---------------- carregamento dos dados ---------------- */

async function carregarTudo() {
  const falha = id('falha');
  try {
    const [resumo, usuarios, doacoes] = await Promise.all([
      api('/doacoes/resumo'),
      api('/usuarios'),
      api('/doacoes'),
    ]);
    estado.resumo = resumo;
    estado.usuarios = Array.isArray(usuarios) ? usuarios : [];
    estado.doacoes = Array.isArray(doacoes) ? doacoes : [];
    falha.hidden = true;
    renderResumo();
    renderUsuarios();
    renderDoacoes();
    preencherDoadores();
  } catch (erro) {
    definirStatus('offline');
    id('falha-texto').textContent = erro.message;
    falha.hidden = false;
  }
}

function renderResumo() {
  const resumo = estado.resumo || { totalDoacoes: 0, totalQuantidade: 0, itensDistintos: 0 };
  id('metric-doacoes').textContent = numero(resumo.totalDoacoes);
  id('metric-quantidade').textContent = numero(resumo.totalQuantidade);
  id('metric-tipos').textContent = numero(resumo.itensDistintos);
}

function nomeDoDoador(usuarioId) {
  const doador = estado.usuarios.find((u) => u.id === usuarioId);
  return doador ? doador.nome : null;
}

function renderUsuarios() {
  const corpo = id('usuarios-corpo');
  corpo.replaceChildren();

  const vazio = estado.usuarios.length === 0;
  id('usuarios-vazio').hidden = !vazio;
  id('tabela-usuarios').hidden = vazio;

  for (const usuario of estado.usuarios) {
    const linha = criar('tr');

    const celulaDoador = criar('td');
    celulaDoador.append(
      criar('span', 'cell-strong', usuario.nome),
      criar('span', 'cell-sub', usuario.email),
    );

    const celulaEnderecos = criar('td');
    const enderecos = usuario.enderecos || [];
    if (enderecos.length === 0) {
      celulaEnderecos.append(criar('span', 'cell-sub', 'Sem endereço cadastrado'));
    } else {
      for (const endereco of enderecos) {
        celulaEnderecos.append(
          criar('span', 'cell-sub', `${endereco.rua} — ${endereco.cidade}/${endereco.estado}`),
        );
      }
    }

    const celulaAcao = criar('td', 'cell-acao');
    const botao = criar('button', 'link-danger', 'Excluir');
    botao.type = 'button';
    botao.setAttribute('aria-label', `Excluir o doador ${usuario.nome}`);
    botao.addEventListener('click', () => excluirUsuario(usuario));
    celulaAcao.append(botao);

    linha.append(celulaDoador, celulaEnderecos, celulaAcao);
    corpo.append(linha);
  }
}

function renderDoacoes() {
  const corpo = id('doacoes-corpo');
  corpo.replaceChildren();

  const vazio = estado.doacoes.length === 0;
  id('doacoes-vazio').hidden = !vazio;
  id('tabela-doacoes').hidden = vazio;

  const ordenadas = [...estado.doacoes].sort((a, b) => {
    const dataA = a.dataDoacao || '';
    const dataB = b.dataDoacao || '';
    if (dataA !== dataB) return dataB.localeCompare(dataA);
    return String(a.item || '').localeCompare(String(b.item || ''), 'pt-BR');
  });

  for (const doacao of ordenadas) {
    const linha = criar('tr');

    const celulaData = criar('td');
    celulaData.append(criar('span', 'cell-sub', formatarData(doacao.dataDoacao)));

    const celulaItem = criar('td');
    celulaItem.append(criar('span', 'cell-strong', doacao.item));

    const celulaQtd = criar('td', 'cell-qtd');
    celulaQtd.textContent = numero(doacao.quantidade);

    const celulaDoador = criar('td');
    const nome = nomeDoDoador(doacao.usuarioId);
    if (nome) {
      celulaDoador.append(criar('span', 'cell-sub', nome));
    } else {
      celulaDoador.append(
        criar('span', 'cell-sub', 'Doador removido'),
        criar('span', 'cell-sub', doacao.usuarioId || '—'),
      );
    }

    const celulaAcao = criar('td', 'cell-acao');
    const botao = criar('button', 'link-danger', 'Excluir');
    botao.type = 'button';
    botao.setAttribute('aria-label', `Excluir a doação de ${doacao.item}`);
    botao.addEventListener('click', () => excluirDoacao(doacao));
    celulaAcao.append(botao);

    linha.append(celulaData, celulaItem, celulaQtd, celulaDoador, celulaAcao);
    corpo.append(linha);
  }
}

function preencherDoadores() {
  const select = id('d-usuario');
  const selecionado = select.value;
  select.replaceChildren();

  const inicial = criar('option', null, 'Selecione um doador');
  inicial.value = '';
  select.append(inicial);

  for (const usuario of estado.usuarios) {
    const opcao = criar('option', null, `${usuario.nome} — ${usuario.email}`);
    opcao.value = usuario.id;
    select.append(opcao);
  }

  const aindaExiste = estado.usuarios.some((u) => u.id === selecionado);
  select.value = aindaExiste ? selecionado : '';
}

/* ---------------- erros de formulário ---------------- */

function limparErros(formulario) {
  for (const campo of formulario.querySelectorAll('[aria-invalid="true"]')) {
    campo.removeAttribute('aria-invalid');
  }
  for (const erro of formulario.querySelectorAll('.field__error')) {
    erro.textContent = '';
    erro.hidden = true;
  }
}

function mostrarErroCampo(formulario, chave, mensagem) {
  const erro = formulario.querySelector(`[data-erro="${chave}"]`);
  const campo = formulario.querySelector(`[data-campo="${chave}"]`);

  if (erro) {
    erro.textContent = mensagem;
    erro.hidden = false;
  }
  if (campo) {
    campo.setAttribute('aria-invalid', 'true');
    if (!campo.getAttribute('aria-describedby') && erro && erro.id) {
      campo.setAttribute('aria-describedby', erro.id);
    }
  }
  return Boolean(erro || campo);
}

function normalizarChave(chave) {
  return String(chave).replace(/\[(\d+)\]\./g, '-$1-');
}

// Limpa o erro do campo assim que a pessoa começa a corrigir
for (const formulario of [id('form-usuario'), id('form-doacao')]) {
  formulario.addEventListener('input', (evento) => {
    const campo = evento.target;
    if (campo.getAttribute('aria-invalid') !== 'true') return;
    campo.removeAttribute('aria-invalid');
    const chave = campo.dataset.campo;
    const erro = chave ? formulario.querySelector(`[data-erro="${chave}"]`) : null;
    if (erro) {
      erro.textContent = '';
      erro.hidden = true;
    }
  });
}

function tratarErroDeFormulario(erro, formulario) {
  if (erro instanceof ApiError && erro.corpo && typeof erro.corpo === 'object' && !erro.corpo.erro) {
    let marcouAlgum = false;
    for (const [chave, mensagem] of Object.entries(erro.corpo)) {
      if (typeof mensagem !== 'string') continue;
      if (mostrarErroCampo(formulario, normalizarChave(chave), mensagem)) marcouAlgum = true;
    }
    if (marcouAlgum) {
      toast('Confira os campos destacados.', 'erro');
      return;
    }
  }
  toast(erro.message, 'erro');
}

/* ---------------- endereços dinâmicos ---------------- */

function reindexarEnderecos() {
  const blocos = [...id('enderecos-lista').children];
  blocos.forEach((bloco, indice) => {
    bloco.querySelector('.endereco__numero').textContent = `Endereço ${indice + 1}`;
    for (const nomeCampo of ['rua', 'cidade', 'estado']) {
      const chave = `enderecos-${indice}-${nomeCampo}`;
      const campo = bloco.querySelector(`[data-campo-base="${nomeCampo}"]`);
      const erro = bloco.querySelector(`[data-erro-base="${nomeCampo}"]`);
      campo.dataset.campo = chave;
      campo.id = `endereco-${indice}-${nomeCampo}`;
      erro.dataset.erro = chave;
      erro.id = `erro-${chave}`;
      const rotulo = campo.closest('.field').querySelector('label');
      if (rotulo) rotulo.setAttribute('for', campo.id);
    }
  });
}

function adicionarEndereco() {
  const bloco = document.createElement('div');
  bloco.className = 'endereco';
  bloco.innerHTML = `
    <div class="endereco__topo">
      <span class="endereco__numero"></span>
      <button class="link-danger" type="button" data-remover>Remover</button>
    </div>
    <div class="field">
      <label>Rua e número</label>
      <input type="text" maxlength="100" data-campo-base="rua" autocomplete="off">
      <p class="field__error" data-erro-base="rua" hidden></p>
    </div>
    <div class="field-row">
      <div class="field">
        <label>Cidade</label>
        <input type="text" maxlength="50" data-campo-base="cidade" autocomplete="off">
        <p class="field__error" data-erro-base="cidade" hidden></p>
      </div>
      <div class="field">
        <label>UF</label>
        <input type="text" maxlength="2" data-campo-base="estado" autocomplete="off">
        <p class="field__error" data-erro-base="estado" hidden></p>
      </div>
    </div>`;

  const uf = bloco.querySelector('[data-campo-base="estado"]');
  uf.addEventListener('input', () => {
    uf.value = uf.value.toUpperCase();
  });

  bloco.querySelector('[data-remover]').addEventListener('click', () => {
    bloco.remove();
    reindexarEnderecos();
  });

  id('enderecos-lista').append(bloco);
  reindexarEnderecos();
}

function lerEnderecos(formulario) {
  const blocos = [...id('enderecos-lista').children];
  const enderecos = [];

  for (const [indice, bloco] of blocos.entries()) {
    const valor = (nomeCampo) =>
      bloco.querySelector(`[data-campo-base="${nomeCampo}"]`).value.trim();

    const rua = valor('rua');
    const cidade = valor('cidade');
    const estado = valor('estado').toUpperCase();

    if (!rua && !cidade && !estado) continue;

    if (!rua || !cidade || !estado) {
      const faltando = !rua ? 'rua' : !cidade ? 'cidade' : 'estado';
      mostrarErroCampo(
        formulario,
        `enderecos-${indice}-${faltando}`,
        'Preencha rua, cidade e UF — ou remova este endereço.',
      );
      return null;
    }

    if (estado.length !== 2) {
      mostrarErroCampo(formulario, `enderecos-${indice}-estado`, 'Use a sigla do estado com 2 letras (ex.: PR).');
      return null;
    }

    enderecos.push({ rua, cidade, estado });
  }

  return enderecos;
}

/* ---------------- ações ---------------- */

function alternarBotao(botao, ocupado, textoOcupado) {
  if (ocupado) {
    botao.dataset.textoOriginal = botao.textContent;
    botao.textContent = textoOcupado;
    botao.disabled = true;
  } else {
    botao.textContent = botao.dataset.textoOriginal || botao.textContent;
    botao.disabled = false;
  }
}

id('form-usuario').addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const formulario = evento.currentTarget;
  limparErros(formulario);

  const enderecos = lerEnderecos(formulario);
  if (enderecos === null) {
    toast('Confira os endereços destacados.', 'erro');
    return;
  }

  const dados = {
    nome: id('u-nome').value.trim(),
    email: id('u-email').value.trim(),
    enderecos,
  };

  const botao = id('btn-usuario');
  alternarBotao(botao, true, 'Cadastrando…');
  try {
    await api('/usuarios', { method: 'POST', body: dados });
    formulario.reset();
    id('enderecos-lista').replaceChildren();
    adicionarEndereco();
    toast('Doador cadastrado.');
    await carregarTudo();
  } catch (erro) {
    tratarErroDeFormulario(erro, formulario);
  } finally {
    alternarBotao(botao, false);
  }
});

id('form-doacao').addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const formulario = evento.currentTarget;
  limparErros(formulario);

  const dados = {
    usuarioId: id('d-usuario').value,
    item: id('d-item').value.trim(),
    quantidade: Number(id('d-quantidade').value),
    dataDoacao: id('d-data').value || null,
  };

  const botao = id('btn-doacao');
  alternarBotao(botao, true, 'Registrando…');
  try {
    await api('/doacoes', { method: 'POST', body: dados });
    formulario.reset();
    id('d-quantidade').value = '1';
    id('d-data').value = hojeIso();
    toast('Doação registrada.');
    await carregarTudo();
  } catch (erro) {
    tratarErroDeFormulario(erro, formulario);
  } finally {
    alternarBotao(botao, false);
  }
});

async function excluirUsuario(usuario) {
  const doacoesDoDoador = estado.doacoes.filter((d) => d.usuarioId === usuario.id).length;
  let aviso = '';
  if (doacoesDoDoador === 1) {
    aviso = '\n\n1 doação registrada para este doador continua no banco.';
  } else if (doacoesDoDoador > 1) {
    aviso = `\n\n${doacoesDoDoador} doações registradas para este doador continuam no banco.`;
  }

  if (!window.confirm(`Excluir o doador "${usuario.nome}"?${aviso}`)) return;

  try {
    await api(`/usuarios/${encodeURIComponent(usuario.id)}`, { method: 'DELETE' });
    toast('Doador excluído.');
    await carregarTudo();
  } catch (erro) {
    toast(erro.message, 'erro');
  }
}

async function excluirDoacao(doacao) {
  const nome = nomeDoDoador(doacao.usuarioId);
  const detalhe = nome ? ` de ${nome}` : '';
  if (!window.confirm(`Excluir a doação de ${doacao.item}${detalhe}, de ${formatarData(doacao.dataDoacao)}?`)) return;

  try {
    await api(`/doacoes/${encodeURIComponent(doacao.id)}`, { method: 'DELETE' });
    toast('Doação excluída.');
    await carregarTudo();
  } catch (erro) {
    toast(erro.message, 'erro');
  }
}

/* ---------------- inicialização ---------------- */

id('add-endereco').addEventListener('click', adicionarEndereco);
id('tentar-novamente').addEventListener('click', () => {
  definirStatus('connecting');
  carregarTudo();
});

// Clicar na quantidade seleciona o valor para não virar "17" ao digitar "7"
const campoQuantidade = id('d-quantidade');
campoQuantidade.addEventListener('focus', () => campoQuantidade.select());

definirStatus('connecting');
id('d-data').value = hojeIso();
adicionarEndereco();
carregarTudo();
