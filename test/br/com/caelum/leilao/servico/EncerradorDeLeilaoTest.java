package br.com.caelum.leilao.servico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;


public class EncerradorDeLeilaoTest {

	private EnviadorDeEmail carteiro;
	private RepositorioDeLeiloes dao;
	private EncerradorDeLeilao encerrador;
	private Calendar data;
	
	@Before
	public void inicializa(){
		carteiro = mock(EnviadorDeEmail.class);
		dao = mock(RepositorioDeLeiloes.class);
		encerrador = new EncerradorDeLeilao(dao, carteiro);
		data = Calendar.getInstance();
	}
	
	@After
	public void finaliza(){
		carteiro = null;
		dao = null;
		encerrador = null;
		data = null;
	}
	
	@Test
	public void deveEncerrarLeiloesQueComecaramAMaisDeUmaSemana(){
		data.set(1999, 1, 20);
		
		Leilao leilao1 = new CriadorDeLeilao().para("TV").naData(data).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Telefone").naData(data).constroi();
		List<Leilao> leiloesAntigos = Arrays.asList(leilao1, leilao2);
		
		when(dao.correntes()).thenReturn(leiloesAntigos);
		
		encerrador.encerra();
		
		assertEquals(2, encerrador.getTotalEncerrados());
		assertTrue(leilao1.isEncerrado());
		assertTrue(leilao2.isEncerrado());
		
	}
	
	@Test
	public void naoDeveEncerrarLeiloesQueComecaramOntem(){
		data.add(Calendar.DAY_OF_MONTH, -1);
		
		Leilao leilao1 = new CriadorDeLeilao().para("TV").naData(data).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Telefone").naData(data).constroi();
		List<Leilao> leiloesAntigos = Arrays.asList(leilao1, leilao2);
		
		when(dao.correntes()).thenReturn(leiloesAntigos);
		
		encerrador.encerra();
		
		assertFalse(leilao1.isEncerrado());
		assertFalse(leilao2.isEncerrado());
		assertEquals(0, encerrador.getTotalEncerrados());
	}
	
	@Test
	public void casoNaoExistaLeilaoOEncerradorNaoDeveFazerNada(){
		data.add(Calendar.DAY_OF_MONTH, -1);
		
		List<Leilao> leiloesAntigos = Arrays.asList();
		
		when(dao.correntes()).thenReturn(leiloesAntigos);
		
		encerrador.encerra();
		
		assertEquals(0, encerrador.getTotalEncerrados());
	}
	
	@Test
	public void deveAtualizarLeiloesEncerrados(){
		data.set(1999, 1,20);
		
		Leilao leilao1 = new CriadorDeLeilao().para("TV").naData(data).constroi();
		
		when(dao.correntes()).thenReturn(Arrays.asList(leilao1));
		
		encerrador.encerra();
		
		verify(dao,times(1)).atualiza(leilao1);
	}
	
	@Test
    public void naoDeveEncerrarLeiloesQueComecaramMenosDeUmaSemanaAtras() {

		data.add(Calendar.DAY_OF_MONTH, -1);

        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(data).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(data).constroi();

        when(dao.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

        encerrador.encerra();

        assertEquals(0, encerrador.getTotalEncerrados());
        assertFalse(leilao1.isEncerrado());
        assertFalse(leilao2.isEncerrado());

        verify(dao, never()).atualiza(leilao1);
        verify(dao, never()).atualiza(leilao2);
        verify(dao, atLeast(0)).atualiza(leilao1);
        verify(dao, atMost(0)).atualiza(leilao1);
        verify(dao, atLeastOnce()).correntes();        
    }
	
	@Test
    public void deveEnviarEmailAposSalvarNoBanco() {
        
		data.add(Calendar.DAY_OF_MONTH, -10);
        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(data).constroi();
        when(dao.correntes()).thenReturn(Arrays.asList(leilao1));
        encerrador.encerra();

        InOrder inOrder = inOrder(dao, carteiro);
        inOrder.verify(dao, times(1)).atualiza(leilao1);
        inOrder.verify(carteiro, times(1)).envia(leilao1);
        
    }
	
	@Test
	public void deveContinuarAExecucaoMesmoQuandoDaoFalha(){
		data.add(Calendar.DAY_OF_MONTH, -10);
		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(data).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("TV de LCD").naData(data).constroi();
		
		when(dao.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		doThrow(new RuntimeException()).when(dao).atualiza(leilao1);
		
		encerrador.encerra();
		
		verify(dao).atualiza(leilao1);
		verify(carteiro, atMost(0)).envia(leilao1);
		verify(dao).atualiza(leilao2);
		verify(carteiro).envia(leilao2);
		
	}
	
	@Test
	public void deveContinuarAExecucaoMesmoQuandoCarteiroFalha(){
		data.add(Calendar.DAY_OF_MONTH, -10);
		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(data).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("TV de LCD").naData(data).constroi();
		
		when(dao.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		doThrow(new RuntimeException()).when(carteiro).envia(leilao1);
		
		encerrador.encerra();
		
		verify(dao).atualiza(leilao1);
		verify(carteiro).envia(leilao1);
		verify(dao).atualiza(leilao2);
		verify(carteiro).envia(leilao2);
		
	}
	
	@Test
	public void naoDeveEnviarEmailCasoTodosOsDaosFalhem(){
		data.add(Calendar.DAY_OF_MONTH, -10);
		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(data).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("TV de LCD").naData(data).constroi();
		
		when(dao.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		doThrow(new RuntimeException()).when(dao).atualiza(any(Leilao.class));
		
		encerrador.encerra();
		
		verify(carteiro, never()).envia(any(Leilao.class));
	}
}
