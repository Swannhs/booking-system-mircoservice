import { Test, TestingModule } from '@nestjs/testing';
import { getModelToken } from '@nestjs/mongoose';
import { EventEmitter2 } from '@nestjs/event-emitter';
import { UsersService } from './users.service';
import { User, UserDocument, UserRole, UserStatus } from './schemas/user.schema';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { ConflictException, NotFoundException } from '@nestjs/common';

describe('UsersService', () => {
  let service: UsersService;
  let mockUserModel: any;
  let mockEventEmitter: jest.Mocked<EventEmitter2>;

  const mockUser = {
    _id: 'userId123',
    email: 'test@example.com',
    firstName: 'John',
    lastName: 'Doe',
    password: 'hashedPassword',
    role: UserRole.USER,
    status: UserStatus.ACTIVE,
    toObject: jest.fn().mockReturnValue({
      _id: 'userId123',
      email: 'test@example.com',
      firstName: 'John',
      lastName: 'Doe',
      role: UserRole.USER,
      status: UserStatus.ACTIVE,
    }),
  };

  beforeEach(async () => {
    mockUserModel = {
      findOne: jest.fn(),
      findById: jest.fn(),
      find: jest.fn(),
      countDocuments: jest.fn(),
      findByIdAndUpdate: jest.fn(),
      save: jest.fn(),
      exec: jest.fn(),
    };

    mockEventEmitter = {
      emit: jest.fn(),
    } as any;

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        UsersService,
        {
          provide: getModelToken(User.name),
          useValue: mockUserModel,
        },
        {
          provide: EventEmitter2,
          useValue: mockEventEmitter,
        },
      ],
    }).compile();

    service = module.get<UsersService>(UsersService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('create', () => {
    it('should create a new user successfully', async () => {
      const createUserDto: CreateUserDto = {
        email: 'test@example.com',
        password: 'password123',
        firstName: 'John',
        lastName: 'Doe',
      };

      mockUserModel.findOne.mockResolvedValue(null);
      mockUserModel.save.mockResolvedValue(mockUser);

      const result = await service.create(createUserDto);

      expect(mockUserModel.findOne).toHaveBeenCalledWith({ email: createUserDto.email });
      expect(mockUserModel.save).toHaveBeenCalled();
      expect(mockEventEmitter.emit).toHaveBeenCalledWith('user.registered', {
        userId: 'userId123',
        email: 'test@example.com',
        firstName: 'John',
        lastName: 'Doe',
        role: UserRole.USER,
      });
      expect(result).toEqual(mockUser);
    });

    it('should throw ConflictException if email already exists', async () => {
      const createUserDto: CreateUserDto = {
        email: 'existing@example.com',
        password: 'password123',
        firstName: 'John',
        lastName: 'Doe',
      };

      mockUserModel.findOne.mockResolvedValue(mockUser);

      await expect(service.create(createUserDto)).rejects.toThrow(ConflictException);
      expect(mockUserModel.save).not.toHaveBeenCalled();
    });
  });

  describe('findByEmail', () => {
    it('should return user by email', async () => {
      mockUserModel.findOne.mockResolvedValue(mockUser);

      const result = await service.findByEmail('test@example.com');

      expect(mockUserModel.findOne).toHaveBeenCalledWith({ email: 'test@example.com' });
      expect(result).toEqual(mockUser);
    });

    it('should return null if user not found', async () => {
      mockUserModel.findOne.mockResolvedValue(null);

      const result = await service.findByEmail('nonexistent@example.com');

      expect(result).toBeNull();
    });
  });

  describe('findById', () => {
    it('should return user by id', async () => {
      mockUserModel.findById.mockResolvedValue(mockUser);

      const result = await service.findById('userId123');

      expect(mockUserModel.findById).toHaveBeenCalledWith('userId123');
      expect(result).toEqual(mockUser);
    });

    it('should throw NotFoundException if user not found', async () => {
      mockUserModel.findById.mockResolvedValue(null);

      await expect(service.findById('nonexistentId')).rejects.toThrow(NotFoundException);
    });
  });

  describe('findAll', () => {
    it('should return paginated users', async () => {
      const users = [mockUser];
      mockUserModel.find.mockReturnValue({
        skip: jest.fn().mockReturnValue({
          limit: jest.fn().mockReturnValue({
            sort: jest.fn().mockReturnValue({
              exec: jest.fn().mockResolvedValue(users),
            }),
          }),
        }),
      });
      mockUserModel.countDocuments.mockResolvedValue(1);

      const result = await service.findAll(0, 10, {});

      expect(result.users).toEqual(users);
      expect(result.total).toBe(1);
    });

    it('should apply filters correctly', async () => {
      const filters = { email: 'test', role: UserRole.ADMIN };
      mockUserModel.find.mockReturnValue({
        skip: jest.fn().mockReturnValue({
          limit: jest.fn().mockReturnValue({
            sort: jest.fn().mockReturnValue({
              exec: jest.fn().mockResolvedValue([]),
            }),
          }),
        }),
      });
      mockUserModel.countDocuments.mockResolvedValue(0);

      await service.findAll(0, 10, filters);

      expect(mockUserModel.find).toHaveBeenCalledWith({
        email: { $regex: 'test', $options: 'i' },
        role: UserRole.ADMIN,
      });
    });
  });

  describe('update', () => {
    it('should update user successfully', async () => {
      const updateUserDto: UpdateUserDto = {
        firstName: 'Jane',
        phone: '+1234567890',
      };

      const updatedUser = { ...mockUser, firstName: 'Jane', phone: '+1234567890' };
      mockUserModel.findByIdAndUpdate.mockResolvedValue(updatedUser);

      const result = await service.update('userId123', updateUserDto);

      expect(mockUserModel.findByIdAndUpdate).toHaveBeenCalledWith(
        'userId123',
        updateUserDto,
        { new: true }
      );
      expect(mockEventEmitter.emit).toHaveBeenCalledWith('user.updated', {
        userId: 'userId123',
        updatedFields: updateUserDto,
      });
      expect(result).toEqual(updatedUser);
    });

    it('should throw NotFoundException if user not found', async () => {
      mockUserModel.findByIdAndUpdate.mockResolvedValue(null);

      await expect(service.update('nonexistentId', {})).rejects.toThrow(NotFoundException);
    });
  });

  describe('remove', () => {
    it('should deactivate user successfully', async () => {
      const deactivatedUser = { ...mockUser, status: UserStatus.INACTIVE };
      mockUserModel.findByIdAndUpdate.mockResolvedValue(deactivatedUser);

      await service.remove('userId123');

      expect(mockUserModel.findByIdAndUpdate).toHaveBeenCalledWith(
        'userId123',
        { status: UserStatus.INACTIVE },
        { new: true }
      );
      expect(mockEventEmitter.emit).toHaveBeenCalledWith('user.deactivated', {
        userId: 'userId123',
      });
    });

    it('should throw NotFoundException if user not found', async () => {
      mockUserModel.findByIdAndUpdate.mockResolvedValue(null);

      await expect(service.remove('nonexistentId')).rejects.toThrow(NotFoundException);
    });
  });

  describe('validatePassword', () => {
    it('should validate correct password', async () => {
      const plainPassword = 'password123';
      const hashedPassword = '$2a$12$hashedPassword'; // bcrypt hash format

      // Mock bcrypt.compare to return true
      const bcrypt = require('bcryptjs');
      jest.spyOn(bcrypt, 'compare').mockResolvedValue(true);

      const result = await service.validatePassword(plainPassword, hashedPassword);

      expect(result).toBe(true);
    });

    it('should reject incorrect password', async () => {
      const plainPassword = 'wrongpassword';
      const hashedPassword = '$2a$12$hashedPassword';

      const bcrypt = require('bcryptjs');
      jest.spyOn(bcrypt, 'compare').mockResolvedValue(false);

      const result = await service.validatePassword(plainPassword, hashedPassword);

      expect(result).toBe(false);
    });
  });

  describe('updateRefreshToken', () => {
    it('should update refresh token', async () => {
      const refreshToken = 'newRefreshToken';
      mockUserModel.findByIdAndUpdate.mockResolvedValue(mockUser);

      await service.updateRefreshToken('userId123', refreshToken);

      expect(mockUserModel.findByIdAndUpdate).toHaveBeenCalledWith(
        'userId123',
        { refreshToken: expect.any(String) } // Should be hashed
      );
    });

    it('should clear refresh token when null provided', async () => {
      mockUserModel.findByIdAndUpdate.mockResolvedValue(mockUser);

      await service.updateRefreshToken('userId123', null);

      expect(mockUserModel.findByIdAndUpdate).toHaveBeenCalledWith(
        'userId123',
        { refreshToken: null }
      );
    });
  });
});