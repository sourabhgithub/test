import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.File;

class DecryptIdPasswordFileTest {

    @Mock
    private EncryptIdPasswordFile encryptIdPasswordFile;

    @Mock
    private Decrypt decrypt;

    private DecryptIdPasswordFile decryptIdPasswordFile;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);  // Initialize mocks
        decryptIdPasswordFile = new DecryptIdPasswordFile("env", "fileName");
    }

    @Test
    void testConstructor_FileNotFound() throws Exception {
        // Arrange: Mocking the behavior of getFile and exists
        File mockFile = mock(File.class);
        when(encryptIdPasswordFile.getFile("env", "fileName")).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(false);

        // Act & Assert: Test if exception is thrown when file is not found
        Exception exception = assertThrows(Exception.class, () -> {
            decryptIdPasswordFile = new DecryptIdPasswordFile("env", "fileName");
        });

        assertTrue(exception.getMessage().contains("Unable to find"));
    }

    @Test
    void testConstructor_Success() throws Exception {
        // Arrange: Mocking the behavior of getFile and exists
        File mockFile = mock(File.class);
        when(encryptIdPasswordFile.getFile("env", "fileName")).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(true);

        // Act: Initialize the object
        decryptIdPasswordFile = new DecryptIdPasswordFile("env", "fileName");

        // Assert: Check if the object is correctly initialized
        assertNotNull(decryptIdPasswordFile);
    }

    @Test
    void testGetUserName() throws Exception {
        // Arrange: Mocking the data
        Container mockData = mock(Container.class);
        when(decryptIdPasswordFile.getUserName()).thenReturn("mockUser");

        // Act: Call the method
        String userName = decryptIdPasswordFile.getUserName();

        // Assert: Check if the correct username is returned
        assertEquals("mockUser", userName);
    }

    @Test
    void testGetPassword() throws Exception {
        // Arrange: Mocking the data
        Container mockData = mock(Container.class);
        when(decryptIdPasswordFile.getPassword()).thenReturn("mockPassword");

        // Act: Call the method
        String password = decryptIdPasswordFile.getPassword();

        // Assert: Check if the correct password is returned
        assertEquals("mockPassword", password);
    }
}
