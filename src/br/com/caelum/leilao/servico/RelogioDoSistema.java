package br.com.caelum.leilao.servico;

import java.util.Calendar;

public class RelogioDoSistema implements Relogio{

	@Override
	public Calendar hoje() {
		return Calendar.getInstance();
	}

}
