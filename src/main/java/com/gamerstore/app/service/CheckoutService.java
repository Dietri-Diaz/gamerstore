package com.gamerstore.app.service;

import com.gamerstore.app.dto.CheckoutClienteDTO;
import com.gamerstore.app.dto.CheckoutPagoDTO;
import com.gamerstore.app.dto.CheckoutRequest;
import com.gamerstore.app.dto.CheckoutResponse;
import com.gamerstore.app.model.Cliente;
import com.gamerstore.app.model.Pago;
import com.gamerstore.app.model.Pedido;
import com.gamerstore.app.repository.ClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Orquesta la compra desde la tienda pública: alta/actualización de cliente, pedido y pago. */
@Service
public class CheckoutService {

    private final ClienteRepository clienteRepository;
    private final PedidoService pedidoService;
    private final PagoService pagoService;
    private final StripeService stripeService;
    private final ComprobanteService comprobanteService;

    // Inyecta el repositorio de clientes y los services de pedido, pago, Stripe y comprobantes.
    public CheckoutService(ClienteRepository clienteRepository, PedidoService pedidoService,
                           PagoService pagoService, StripeService stripeService,
                           ComprobanteService comprobanteService) {
        this.clienteRepository = clienteRepository;
        this.pedidoService = pedidoService;
        this.pagoService = pagoService;
        this.stripeService = stripeService;
        this.comprobanteService = comprobanteService;
    }

    /**
     * Procesa una compra completa desde la tienda: busca o crea al cliente por DNI, arma el
     * pedido (valida y descuenta stock) y cobra con el método elegido (Yape o tarjeta).
     */
    @Transactional
    public CheckoutResponse comprar(CheckoutRequest req) {
        Cliente cliente = obtenerOCrearCliente(req.cliente());

        // Los datos de entrega (recojo o delivery + dirección) viajan hasta el pedido; el
        // service valida que un DELIVERY traiga dirección de verdad.
        Pedido pedido = pedidoService.crear(cliente.getId(), req.pago().metodo(), req.items(),
                req.entrega().tipo(), req.entrega().direccion(), req.entrega().referencia());

        Pago pago = cobrar(pedido.getId(), req.pago());

        // Si el banco rechaza el pago, esta excepción revierte TODA la transacción (el método es
        // @Transactional): no queda pedido creado, el stock descontado se restaura y el pago
        // registrado tampoco se guarda. El comprador puede reintentar sin dejar nada a medias.
        if ("RECHAZADO".equals(pago.getEstado())) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Pago rechazado por el banco. Revisa los datos de tu tarjeta e inténtalo de nuevo.");
        }

        // El pago aprobado ya emitió su boleta (PagoService -> ComprobanteService.emitir); la
        // buscamos solo para devolver su código. Si por algo no existiera, no rompemos la compra.
        String comprobanteCodigo = null;
        try {
            comprobanteCodigo = comprobanteService.porPedido(pedido.getId()).getCodigo();
        } catch (ResponseStatusException ignored) {
            // Sin boleta: no debería pasar tras un pago aprobado, pero la compra ya se concretó.
        }

        return new CheckoutResponse(pedido.getCodigo(), pago.getCodigo(), pago.getEstado(),
                pago.getMetodo(), pago.getReferencia(), pedido.getTotal(), cliente.getNombreCompleto(),
                comprobanteCodigo);
    }

    /**
     * Identifica a un comprador que dice "ya soy cliente". Exige que el DNI y el correo
     * coincidan con los guardados: así prellenamos sus datos sin exponer la información
     * de un cliente a cualquiera que solo sepa su DNI.
     */
    public CheckoutClienteDTO verificarCliente(String dni, String email) {
        Cliente c = clienteRepository.findByDni(dni.trim())
                .filter(x -> x.getEmail() != null && x.getEmail().equalsIgnoreCase(email.trim()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No encontramos una cuenta con ese DNI y correo. Puedes continuar como invitado."));
        return new CheckoutClienteDTO(c.getDni(), c.getNombres(), c.getApellidos(),
                c.getTelefono(), c.getEmail(), c.getDireccion());
    }

    // Busca al comprador por su DNI: si ya es cliente, actualiza sus datos de contacto; si no, lo da de alta.
    private Cliente obtenerOCrearCliente(CheckoutClienteDTO c) {
        return clienteRepository.findByDni(c.dni())
                .map(existente -> actualizarContacto(existente, c))
                .orElseGet(() -> crearCliente(c));
    }

    // Actualiza teléfono/email/dirección del cliente existente con los datos no vacíos que llegaron del checkout.
    private Cliente actualizarContacto(Cliente cliente, CheckoutClienteDTO c) {
        if (c.telefono() != null && !c.telefono().isBlank()) cliente.setTelefono(c.telefono());
        if (c.direccion() != null && !c.direccion().isBlank()) cliente.setDireccion(c.direccion());
        if (c.email() != null && !c.email().isBlank()) {
            // Si el email ya pertenece a OTRO cliente, no lo tocamos: no vale la pena romper
            // la compra por el único de email.
            if (!clienteRepository.existsByEmailIgnoreCaseAndIdNot(c.email().trim(), cliente.getId())) {
                cliente.setEmail(c.email());
            }
        }
        return clienteRepository.save(cliente);
    }

    // Da de alta un cliente nuevo con los datos del comprador que llegaron del checkout.
    private Cliente crearCliente(CheckoutClienteDTO c) {
        Cliente cliente = new Cliente();
        cliente.setDni(c.dni());
        cliente.setNombres(c.nombres());
        cliente.setApellidos(c.apellidos());
        cliente.setTelefono(c.telefono());
        cliente.setDireccion(c.direccion());
        // Mismo cuidado que al actualizar: si el email ya pertenece a otro cliente, se omite.
        if (c.email() != null && !c.email().isBlank() && !clienteRepository.existsByEmailIgnoreCase(c.email().trim())) {
            cliente.setEmail(c.email());
        }
        return clienteRepository.save(cliente);
    }

    // Cobra el pedido según el método elegido (ignora mayúsculas/minúsculas).
    private Pago cobrar(Long pedidoId, CheckoutPagoDTO pago) {
        String metodo = pago.metodo() == null ? "" : pago.metodo().trim().toUpperCase();
        return switch (metodo) {
            // Si Stripe está activo y llegó el token del navegador, cobra de verdad; si no,
            // cae a la pasarela simulada (Luhn) para que la demo nunca se caiga sin internet.
            case "TARJETA" -> (stripeService.estaActivo() && pago.paymentMethodId() != null && !pago.paymentMethodId().isBlank())
                    ? pagoService.pagarConStripe(pedidoId, pago.paymentMethodId())
                    : pagoService.pagarConTarjeta(pedidoId, pago.numero(), pago.titular(), pago.vencimiento(), pago.cvv());
            case "YAPE" -> pagoService.pagarConYape(pedidoId, pago.numeroOperacion(), pago.voucher());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Método de pago inválido");
        };
    }
}
