// Array para almacenar la orden
let order = [];

// Funciones para abrir y cerrar modales
function openModal(modalId, url = null, contentId = null) {
    if (url && contentId) {
        loadContent(url, contentId); // Cargar contenido dinámico si se especifica
    }
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = "block";
    } else {
        console.error(`No se encontró el modal con ID: ${modalId}`);
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = "none";
    } else {
        console.error(`No se encontró el modal con ID: ${modalId}`);
    }
}

// Función para cargar contenido dinámico en un modal
function loadContent(url, elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        fetch(url)
            .then(response => response.text())
            .then(data => {
                element.innerHTML = data;
                assignTabLinks(); // Asignar eventos a los tabs después de cargar el contenido
            })
            .catch(error => console.error("Error cargando el contenido:", error));
    } else {
        console.error(`No se encontró el elemento con ID: ${elementId}`);
    }
}

// Agregar elementos a la orden
function addToOrder(itemId, itemName, price) {
    if (isNaN(price)) {
        alert("El precio del elemento no es válido.");
        return;
    }

    const existingItem = order.find(item => item.itemId === itemId);
    if (existingItem) {
        existingItem.quantity += 1;
    } else {
        order.push({ itemId, itemName, price: parseFloat(price), quantity: 1 });
    }

    alert(`${itemName} agregado a la orden.`);
    displayOrder();
}

// Mostrar la orden en el modal "Orden Final"
function displayOrder() {
    const orderContent = document.getElementById("orderContent");
    if (order.length === 0) {
        orderContent.innerHTML = "<p>No hay elementos en la orden.</p>";
    } else {
        orderContent.innerHTML = order
            .map(item => `<p>${item.itemName} x${item.quantity} - $${(item.price * item.quantity).toFixed(2)}</p>`)
            .join("");
    }
}

// Vaciar la orden
function clearOrder() {
    order = [];
    alert("La orden ha sido vaciada.");
    displayOrder();
}

// Abrir el modal de "Orden Final"
function openOrderModal() {
    displayOrder();
    openModal("orderModal");
}

// Abrir el modal de "Pago" y preparar los datos
function openPaymentModal() {
    const totalAmount = order.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2);
    document.getElementById("totalAmount").textContent = `$${totalAmount}`;

    // Crear un campo oculto con los datos de la orden
    const form = document.querySelector("#paymentContent form");
    const existingInput = form.querySelector('input[name="orderData"]');
    if (existingInput) {
        existingInput.remove();
    }

    const orderDataInput = document.createElement("input");
    orderDataInput.type = "hidden";
    orderDataInput.name = "orderData";
    orderDataInput.value = JSON.stringify(order);
    form.appendChild(orderDataInput);

    closeModal("orderModal");
    openModal("paymentModal");
}

// Procesar el pago
function processPayment(event) {
    event.preventDefault();
    alert("Pago procesado exitosamente. ¡Gracias por tu compra!");
    closeModal("paymentModal");
    clearOrder();
}

// Enviar la orden al servidor
function sendOrderToServer() {
    if (order.length === 0) {
        alert("No hay elementos en la orden para enviar.");
        return;
    }

    fetch("http://localhost:7000/order", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ items: order }),
    })
        .then(response => {
            if (response.ok) {
                alert("Orden enviada exitosamente al servidor.");
                clearOrder();
            } else {
                response.text().then(text => alert("Error al enviar la orden: " + text));
            }
        })
        .catch(error => {
            console.error("Error al enviar la orden:", error);
            alert("No se pudo enviar la orden al servidor.");
        });
}

// Asignar acciones a tabs y botones
function assignTabLinks() {
    document.querySelectorAll("#menuContent .tablink").forEach(tab => {
        tab.onclick = function (event) {
            openTab(event, tab.innerText);
        };
    });

    document.querySelectorAll(".order-button").forEach(button => {
        button.onclick = function () {
            const itemId = parseInt(this.dataset.itemId);
            const itemName = this.dataset.itemName;
            const price = parseFloat(this.dataset.price);
            addToOrder(itemId, itemName, price);
        };
    });
}

// Cambiar entre tabs en el menú
function openTab(evt, tabName) {
    document.querySelectorAll(".menu").forEach(menu => (menu.style.display = "none"));
    document.querySelectorAll(".tablink").forEach(tab => tab.classList.remove("w3-red"));

    const tabContent = document.getElementById(tabName);
    if (tabContent) {
        tabContent.style.display = "block";
    } else {
        console.error(`No se encontró el contenido del tab con nombre: ${tabName}`);
    }

    evt.currentTarget.classList.add("w3-red");
}
